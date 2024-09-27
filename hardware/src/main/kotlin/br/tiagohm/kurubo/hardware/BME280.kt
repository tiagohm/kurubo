package br.tiagohm.kurubo.hardware

import br.tiagohm.kurubo.client.Firmata
import br.tiagohm.kurubo.client.TwoWire
import br.tiagohm.kurubo.client.bit3
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.math.min

// https://github.com/adafruit/Adafruit_BME280_Library/blob/master/Adafruit_BME280.cpp

/**
 * BME280: Combined Humidity and Pressure Sensor.
 *
 * @see <a href="https://www.mouser.com/datasheet/2/783/BST-BME280-DS002-1509607.pdf">Datasheet</a>
 */
@Suppress("PrivatePropertyName", "unused")
data class BME280(
    override val firmata: Firmata,
    val address: Int = ADDRESS,
    val mode: Mode = Mode.FORCED,
    override val pollInterval: Long = if (mode == Mode.FORCED) 2500L else 5000L,
    val temperatureSampling: SamplingMode = SamplingMode.X16,
    val pressureSampling: SamplingMode = SamplingMode.X16,
    val humiditySampling: SamplingMode = SamplingMode.X16,
    val filter: FilterMode = FilterMode.OFF,
    val standByDuration: StandByDuration = StandByDuration.MS_0_5,
) : AbstractHardware(firmata), Barometer, Thermometer, Hygrometer, Pollable {

    enum class Mode(@JvmField val bits: Int) {
        SLEEP(0b00),
        FORCED(0b01),
        NORMAL(0b11),
    }

    enum class SamplingMode(@JvmField val bits: Int) {
        NONE(0b000),
        X1(0b001),
        X2(0b010),
        X4(0b011),
        X8(0b100),
        X16(0b101),
    }

    enum class FilterMode(@JvmField val bits: Int) {
        OFF(0b000),
        X2(0b001),
        X4(0b010),
        X8(0b011),
        X16(0b100),
    }

    enum class StandByDuration(@JvmField val bits: Int) {
        MS_0_5(0b000),
        MS_10(0b110),
        MS_20(0b111),
        MS_62_5(0b001),
        MS_125(0b010),
        MS_250(0b011),
        MS_500(0b100),
        MS_1000(0b101),
    }

    @Volatile private var T1 = 0
    @Volatile private var T2 = 0
    @Volatile private var T3 = 0

    @Volatile private var P1 = 0
    @Volatile private var P2 = 0
    @Volatile private var P3 = 0
    @Volatile private var P4 = 0
    @Volatile private var P5 = 0
    @Volatile private var P6 = 0
    @Volatile private var P7 = 0
    @Volatile private var P8 = 0
    @Volatile private var P9 = 0

    @Volatile private var H1 = 0
    @Volatile private var H2 = 0
    @Volatile private var H3 = 0
    @Volatile private var H4 = 0
    @Volatile private var H5 = 0
    @Volatile private var H6 = 0

    @Volatile private var tempFine = 0L
    @Volatile private var forced = false
    @Volatile private var hasHygrometer = false

    private val humidityReg = humiditySampling.bits shl 5
    private val configReg = standByDuration.bits or (filter.bits shl 3)
    private val measurementReg = temperatureSampling.bits or (pressureSampling.bits shl 3) or (mode.bits shl 6)

    private val thermometerListeners = ConcurrentHashMap.newKeySet<ThermometerListener>(1)
    private val barometerListeners = ConcurrentHashMap.newKeySet<BarometerListener>(1)
    private val altimeterListeners = ConcurrentHashMap.newKeySet<AltimeterListener>(1)
    private val hygrometerListeners = ConcurrentHashMap.newKeySet<HygrometerListener>(1)
    private val calibrated = AtomicBoolean()

    init {
        require(address == ADDRESS_ALTERNATE || address == ADDRESS) { "invalid BME280 address" }
    }

    override var name = "BME280"
        private set

    private lateinit var device: TwoWire

    @Volatile override var pressure = 0.0
        private set

    @Volatile override var altitude = 0.0
        private set

    @Volatile override var temperature = 0.0
        private set

    @Volatile override var humidity = 0.0
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

    override fun addHygrometerListener(listener: HygrometerListener) {
        hygrometerListeners.add(listener)
    }

    override fun removeHygrometerListener(listener: HygrometerListener) {
        hygrometerListeners.remove(listener)
    }

    override fun start() {
        super.start()

        if (!::device.isInitialized) {
            with(firmata.twoWireDevice(address)) {
                device = this
            }
        }
    }

    override fun run() {
        if (::device.isInitialized
            && (thermometerListeners.isNotEmpty() || barometerListeners.isNotEmpty() || altimeterListeners.isNotEmpty() || (hasHygrometer && hygrometerListeners.isNotEmpty()))
        ) {
            try {
                if (!calibrated.get() && !readCalibrationData()) return

                if (mode == Mode.FORCED) {
                    if (!forced) {
                        // Put in forced mode.
                        device.tell(byteArrayOf(REGISTER_CONTROL.toByte(), measurementReg.toByte()))
                        forced = true
                        return
                    }
                    // Wait until measurement has been completed.
                    else if (readStatus().bit3) {
                        LOG.warn("measurement has not been completed")
                        return
                    }

                    forced = false
                }

                temperature = readTemperature().takeIf(Double::isFinite) ?: return
                thermometerListeners.forEach { it.onTemperatureChange(this) }

                if (barometerListeners.isNotEmpty() || altimeterListeners.isNotEmpty()) {
                    pressure = readPressure().takeIf(Double::isFinite) ?: return
                    barometerListeners.forEach { it.onPressureChange(this) }

                    if (altimeterListeners.isNotEmpty()) {
                        altitude = Altimeter.estimatedAltitudeFromPressureAndTemperature(pressure, temperature)
                        altimeterListeners.forEach { it.onAltitudeChange(this) }
                    }
                }

                if (hasHygrometer && hygrometerListeners.isNotEmpty()) {
                    humidity = readHumidity().takeIf(Double::isFinite) ?: return
                    hygrometerListeners.forEach { it.onHumidityChange(this) }
                }
            } catch (_: TimeoutException) {
                return
            }
        }
    }

    private fun readCalibrationData(): Boolean {
        val id = readChipID()

        name = if (id in 0x56..0x58) "BMP280" else if (id == 0x60) "BME280" else if (id == 0x61) "BME680" else "BMx???"
        LOG.info("chip id: %02X, model: %s".format(id, name))

        device.tell(byteArrayOf(REGISTER_SOFTRESET.toByte(), 0xB6.toByte()))

        Thread.sleep(10)

        val temp = device.askSync(REGISTER_DIG_T1, 6, 100L)?.data ?: return false
        T1 = (temp[0] or (temp[1] shl 8)) and 0xFFFF
        T2 = (temp[2] or (temp[3] shl 8)).toShort().toInt()
        T3 = (temp[4] or (temp[5] shl 8)).toShort().toInt()

        LOG.debug("T1: {}, T2: {}, T3: {}", T1, T2, T3)

        val pres = device.askSync(REGISTER_DIG_P1, 18, 100L)?.data ?: return false
        P1 = (pres[0] or (pres[1] shl 8)) and 0xFFFF
        P2 = (pres[2] or (pres[3] shl 8)).toShort().toInt()
        P3 = (pres[4] or (pres[5] shl 8)).toShort().toInt()
        P4 = (pres[6] or (pres[7] shl 8)).toShort().toInt()
        P5 = (pres[8] or (pres[9] shl 8)).toShort().toInt()
        P6 = (pres[10] or (pres[11] shl 8)).toShort().toInt()
        P7 = (pres[12] or (pres[13] shl 8)).toShort().toInt()
        P8 = (pres[14] or (pres[15] shl 8)).toShort().toInt()
        P9 = (pres[16] or (pres[17] shl 8)).toShort().toInt()

        LOG.debug("P1: {}, P2: {}, P3: {}, P4:{}, P5: {}, P6: {}, P7: {}, P8: {}, P9: {}", P1, P2, P3, P4, P5, P6, P7, P8, P9)

        try {
            H1 = device.askSync(REGISTER_DIG_H1, 1, 100L)?.data?.get(0)?.and(0xFF) ?: return false
            val h2 = device.askSync(REGISTER_DIG_H2, 2, 100L)?.data ?: return false
            H2 = (h2[2] or (h2[3] shl 8)).toShort().toInt()
            H3 = device.askSync(REGISTER_DIG_H3, 1, 100L)?.data?.get(0)?.and(0xFF) ?: return false
            val h4 = device.askSync(REGISTER_DIG_H4, 2, 100L)?.data ?: return false
            H4 = ((h4[0] shl 4) or (h4[1] and 0xF)).toShort().toInt()
            val h5 = device.askSync(REGISTER_DIG_H5, 2, 100L)?.data ?: return false
            H5 = ((h5[1] shl 4) or (h5[0] shr 4)).toShort().toInt()
            H6 = device.askSync(REGISTER_DIG_H6, 1, 100L)?.data?.get(0)?.and(0xFF) ?: return false

            hasHygrometer = true

            LOG.debug("H1: {}, H2: {}, H3: {}, H4:{}, H5: {}, H6: {}", H1, H2, H3, H4, H5, H6)
        } catch (_: Throwable) {
            LOG.error("failed to read humidity coefficients")
        } finally {
            calibrated.set(true)
            writeSampling()
        }

        return true
    }

    private fun readChipID(): Int {
        return device.askSync(REGISTER_CHIPID, 1, 1000L)?.data?.get(0) ?: 0
    }

    private fun readStatus(): Int {
        return device.askSync(REGISTER_STATUS, 1, 1000L)?.data?.get(0) ?: 0
    }

    private fun writeSampling() {
        // Making sure sensor is in sleep mode before setting configuration as it otherwise may be ignored
        device.tell(byteArrayOf(REGISTER_CONTROL.toByte(), Mode.SLEEP.bits.toByte()))
        device.tell(byteArrayOf(REGISTER_CONTROLHUMID.toByte(), humidityReg.toByte()))
        device.tell(byteArrayOf(REGISTER_CONFIG.toByte(), configReg.toByte()))
        device.tell(byteArrayOf(REGISTER_CONTROL.toByte(), measurementReg.toByte()))
    }

    private fun readTemperature(): Double {
        val temp = device.askSync(REGISTER_TEMPDATA, 3, 1000L)?.data ?: return Double.NaN
        var adc = (temp[0] and 0xFF shl 16) or (temp[1] and 0xFF shl 8) or (temp[2] and 0xFF)

        if (adc == 0x800000) return Double.NaN

        adc = adc shr 4

        var a = (adc / 8) - (T1 * 2)
        a = (a * T2) / 2048
        var b = (adc / 16) - T1
        b = (((b * b) / 4096) * T3) / 16384

        tempFine = (a + b).toLong()

        return ((tempFine * 5 + 128) / 256) / 100.0
    }

    private fun readPressure(): Double {
        val pres = device.askSync(REGISTER_PRESSUREDATA, 3, 1000L)?.data ?: return Double.NaN
        var adc = (pres[0] and 0xFF shl 16) or (pres[1] and 0xFF shl 8) or (pres[2] and 0xFF)

        if (adc == 0x800000) return Double.NaN

        adc = adc shr 4

        var a = tempFine - 128000
        var b = a * a * P6
        b += ((a * P5) * 131072L)
        b += (P4 * 34359738368L)
        a = ((a * a * P3) / 256) + ((a * P2 * 4096))
        a = (140737488355328L + a) * P1 / 8589934592L

        if (a == 0L) {
            return 0.0
        }

        var d = 1048576L - adc
        d = (((d * 2147483648L) - b) * 3125L) / a
        a = (P9 * (d / 8192L) * (d / 8192L)) / 33554432L
        b = (P8 * d) / 524288L
        d = ((d + a + b) / 256) + (P7 * 16)

        return d / 256.0
    }

    private fun readHumidity(): Double {
        val hum = device.askSync(REGISTER_HUMIDDATA, 2, 1000L)?.data ?: return Double.NaN
        val adc = (hum[0] and 0xFF shl 8) or (hum[1] and 0xFF)

        if (adc == 0x8000) return Double.NaN

        val a = tempFine - 76800
        var b = adc * 16384L
        var c = H4 * 1048576L
        var d = H5 * a
        var e = (((b - c) - d) + 16384) / 32768
        b = (a * H6) / 1024
        c = (a * H3) / 2048
        d = ((b * (c + 32768)) / 1024) + 2097152
        b = ((d * (H2)) + 8192) / 16384
        c = e * b
        d = ((c / 32768) * (c / 32768)) / 128
        e = c - ((d * (H1)) / 16)
        e = max(0, min(e, 419430400))
        return (e / 4096) / 1024.0
    }

    companion object {

        const val ADDRESS = 0x77
        const val ADDRESS_ALTERNATE = 0x76

        const val REGISTER_DIG_T1 = 0x88
        const val REGISTER_DIG_T2 = 0x8A
        const val REGISTER_DIG_T3 = 0x8C

        const val REGISTER_DIG_P1 = 0x8E
        const val REGISTER_DIG_P2 = 0x90
        const val REGISTER_DIG_P3 = 0x92
        const val REGISTER_DIG_P4 = 0x94
        const val REGISTER_DIG_P5 = 0x96
        const val REGISTER_DIG_P6 = 0x98
        const val REGISTER_DIG_P7 = 0x9A
        const val REGISTER_DIG_P8 = 0x9C
        const val REGISTER_DIG_P9 = 0x9E

        const val REGISTER_DIG_H1 = 0xA1
        const val REGISTER_DIG_H2 = 0xE1
        const val REGISTER_DIG_H3 = 0xE3
        const val REGISTER_DIG_H4 = 0xE4
        const val REGISTER_DIG_H5 = 0xE5
        const val REGISTER_DIG_H6 = 0xE7

        const val REGISTER_CHIPID = 0xD0
        const val REGISTER_VERSION = 0xD1
        const val REGISTER_SOFTRESET = 0xE0

        const val REGISTER_CAL26 = 0xE1 // R calibration stored in 0xE1-0xF

        const val REGISTER_CONTROLHUMID = 0xF2
        const val REGISTER_STATUS = 0XF3
        const val REGISTER_CONTROL = 0xF4
        const val REGISTER_CONFIG = 0xF5
        const val REGISTER_PRESSUREDATA = 0xF7
        const val REGISTER_TEMPDATA = 0xFA
        const val REGISTER_HUMIDDATA = 0xFD

        private val LOG = LoggerFactory.getLogger(BME280::class.java)
    }
}
