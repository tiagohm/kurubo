package br.tiagohm.kurubo.hardware

import br.tiagohm.kurubo.client.Firmata
import br.tiagohm.kurubo.client.TwoWire
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean

// https://github.com/dotnet/iot/blob/main/src/devices/Bmp180/Bmp180.cs
// https://github.com/rwaldron/johnny-five/blob/main/lib/barometer.js#L45
// https://github.com/adafruit/Adafruit-BMP085-Library

// Tested with Arduino + Adafruit-BMP085-Library
// ac1 = 8167
// ac2 = -1146
// ac3 = -14574
// ac4 = 34285
// ac5 = 24212
// ac6 = 23458
// b1 = 5498
// b2 = 61
// mb = -32768
// mc = -11075
// md = 2432
// Temperature = Raw temp: 32774
// 27.80 *C
// Pressure = Raw temp: 32772
// Raw pressure: 312855
// X1 = 0
// X2 = 0
// B5 = 4447
// B6 = 447
// X1 = 1
// X2 = -251
// B3 = 64836
// X1 = -796
// X2 = 4
// B4 = 34077
// B7 = 1550118750
// p = 90977
// X1 = 5842
// X2 = -10213
// p = 90940
// 90940 Pa

/**
 * BMP180: Digital pressure sensor.
 *
 * @see <a href="https://cdn-shop.adafruit.com/datasheets/BST-BMP180-DS000-09.pdf">Datasheet</a>
 */
@Suppress("PrivatePropertyName")
data class BMP180(
    override val firmata: Firmata,
    val mode: Mode = Mode.ULTRA_LOW_POWER,
    override val pollInterval: Long = 5000L,
) : AbstractHardware(firmata), Barometer, Thermometer, Pollable {

    @Suppress("unused")
    enum class Mode(@JvmField val sleepTimeInMilliseconds: Long) {
        ULTRA_LOW_POWER(5L),
        STANDARD(8L),
        HIGH_RESOLUTION(14L),
        ULTRA_HIGH_RESOLUTION(26L),
    }

    @Volatile private var AC1 = 408
    @Volatile private var AC2 = -72
    @Volatile private var AC3 = -14383
    @Volatile private var AC4 = 32741
    @Volatile private var AC5 = 32757
    @Volatile private var AC6 = 23153

    @Volatile private var B1 = 6190
    @Volatile private var B2 = 4

    @Volatile private var MB = -32768
    @Volatile private var MC = -8711
    @Volatile private var MD = 2868

    private val thermometerListeners = ConcurrentHashMap.newKeySet<ThermometerListener>(1)
    private val barometerListeners = ConcurrentHashMap.newKeySet<BarometerListener>(1)
    private val altimeterListeners = ConcurrentHashMap.newKeySet<AltimeterListener>(1)
    private val calibrated = AtomicBoolean()

    override val name = "BMP180"

    private lateinit var device: TwoWire

    @Volatile override var pressure = 0.0
        private set

    @Volatile override var altitude = 0.0
        private set

    @Volatile override var temperature = 0.0
        private set

    override fun addThermometerListener(listener: ThermometerListener) {
        thermometerListeners.add(listener)
    }

    override fun removeThermometerListener(listener: ThermometerListener) {
        thermometerListeners.remove(listener)
    }

    override fun addBarometerListener(listener: BarometerListener) {
        barometerListeners.add(listener)
    }

    override fun removeBarometerListener(listener: BarometerListener) {
        barometerListeners.remove(listener)
    }

    override fun addAltimeterListener(listener: AltimeterListener) {
        altimeterListeners.add(listener)
    }

    override fun removeAltimeterListener(listener: AltimeterListener) {
        altimeterListeners.remove(listener)
    }

    override fun start() {
        if (!::device.isInitialized) {
            with(firmata.twoWireDevice(ADDRESS)) {
                device = this
            }
        }
    }

    override fun run() {
        if (::device.isInitialized && (thermometerListeners.isNotEmpty() || barometerListeners.isNotEmpty() || altimeterListeners.isNotEmpty())) {
            try {
                if (!calibrated.get() && !readCalibrationData()) return

                val b5 = calculateTrueTemperature()

                if (thermometerListeners.isNotEmpty()) {
                    temperature = (b5 + 8) / 160.0
                    thermometerListeners.forEach { it.onTemperatureChange(this) }
                }

                if (barometerListeners.isNotEmpty() || altimeterListeners.isNotEmpty()) {
                    val b6 = b5 - 4000
                    val k = (b6 * b6) / 4096
                    var x3 = ((B2 * k) + (AC2 * b6)) / 2048
                    val b3 = (((AC1 * 4 + x3) shl mode.ordinal) + 2) / 4
                    var x1 = (AC3 * b6) / 8192
                    val x2 = (B1 * k) / 65536
                    x3 = (x1 + x2 + 2) / 4
                    val b4 = AC4 * ((x3 + 32768).toLong() and 0xFFFFFFFF) / 32768
                    val up = readUncompensatedPressure()
                    val b7 = ((up - b3).toLong() and 0xFFFFFFFF) * (50000 shr mode.ordinal)
                    val p = if ((b7 < 0x80000000)) (b7 * 2) / b4 else (b7 / b4) * 2
                    x1 = ((((p * p) / 65536) * 3038) / 65536).toInt()
                    pressure = (p + (x1 + ((-7357 * p) / 65536) + 3791) / 8).toDouble()
                    barometerListeners.forEach { it.onPressureChange(this) }

                    if (altimeterListeners.isNotEmpty()) {
                        altitude = Altimeter.estimatedAltitudeFromPressureAndTemperature(pressure, temperature)
                        altimeterListeners.forEach { it.onAltitudeChange(this) }
                    }
                }
            } catch (_: TimeoutException) {
                return
            }
        }
    }

    private fun readCalibrationData(): Boolean {
        val data = device.askSync(COEFFICIENTS_REG, 22, 1000L)?.data ?: return false
        AC1 = ((data[0] shl 8) or data[1]).toShort().toInt()
        AC2 = ((data[2] shl 8) or data[3]).toShort().toInt()
        AC3 = ((data[4] shl 8) or data[5]).toShort().toInt()
        AC4 = (data[6] shl 8) or data[7]
        AC5 = (data[8] shl 8) or data[9]
        AC6 = (data[10] shl 8) or data[11]
        B1 = ((data[12] shl 8) or data[13]).toShort().toInt()
        B2 = ((data[14] shl 8) or data[15]).toShort().toInt()
        MB = ((data[16] shl 8) or data[17]).toShort().toInt()
        MC = ((data[18] shl 8) or data[19]).toShort().toInt()
        MD = ((data[20] shl 8) or data[21]).toShort().toInt()

        LOG.debug("AC1={}, AC2={}, AC3={}, AC4={}, AC5={}, AC6={}, B1={}, B2={}, MB={}, MC={}, MD={}", AC1, AC2, AC3, AC4, AC5, AC6, B1, B2, MB, MC, MD)

        calibrated.set(true)

        return true
    }

    private fun readUncompensatedTemperature(): Int {
        device.tell(READ_TEMP)

        Thread.sleep(5)

        val temp = device.askSync(TEMP_DATA_REG, 2, 1000L)?.data ?: return 0
        return (temp[0] shl 8) or temp[1]
    }

    private fun readUncompensatedPressure(): Int {
        device.tell(byteArrayOf(CONTROL_REG.toByte(), (READ_PRES_CMD or (mode.ordinal shl 6)).toByte()))

        Thread.sleep(mode.sleepTimeInMilliseconds)

        val pres = device.askSync(PRES_DATA_REG, 3, 1000L)?.data ?: return 0
        return ((pres[0] shl 16) or (pres[1] shl 8) or pres[2]) shr (8 - mode.ordinal)
    }

    private fun calculateTrueTemperature(): Int {
        val ut = readUncompensatedTemperature()

        // Calculations below are taken straight from section 3.5 of the datasheet.
        val x1 = (ut - AC6) * AC5 / 32768
        val x2 = MC * 2048 / (x1 + MD)

        return x1 + x2
    }

    companion object {

        const val ADDRESS = 0x77

        private const val COEFFICIENTS_REG = 0xAA
        private const val CONTROL_REG = 0xF4
        private const val TEMP_DATA_REG = 0xF6
        private const val PRES_DATA_REG = 0xF6

        private const val READ_TEMP_CMD = 0x2E
        private const val READ_PRES_CMD = 0x34

        private val LOG = LoggerFactory.getLogger(BMP180::class.java)

        private val READ_TEMP = byteArrayOf(CONTROL_REG.toByte(), READ_TEMP_CMD.toByte())
    }
}
