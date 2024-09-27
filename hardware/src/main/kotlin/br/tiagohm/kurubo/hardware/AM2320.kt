package br.tiagohm.kurubo.hardware

import br.tiagohm.kurubo.client.*
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

// https://github.com/dotnet/iot/blob/main/src/devices/Am2320/Am2320.cs
// https://github.com/adafruit/Adafruit_AM2320

/**
 * AM2320: Digital Temperature and Humidity Sensor.
 *
 * @see <a href="https://cdn-shop.adafruit.com/product-files/3721/AM2320.pdf">Datasheet</a>
 */
@Suppress("unused")
data class AM2320(
    override val firmata: Firmata,
    override val pollInterval: Long = 5000L,
) : AbstractHardware(firmata), Thermometer, Hygrometer, TwoWireListener, Pollable {

    private val thermometerListeners = ConcurrentHashMap.newKeySet<ThermometerListener>(1)
    private val hygrometerListeners = ConcurrentHashMap.newKeySet<HygrometerListener>(1)

    override val name = "AM2320"

    private lateinit var device: TwoWire

    @Volatile override var humidity = 0.0
        private set

    @Volatile override var temperature = 0.0
        private set

    override fun addThermometerListener(listener: ThermometerListener) {
        thermometerListeners.add(listener)
    }

    override fun removeThermometerListener(listener: ThermometerListener) {
        thermometerListeners.remove(listener)
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
            with(firmata.twoWireDevice(ADDRESS)) {
                addListener(this@AM2320)
                device = this
            }
        }
    }

    override fun run() {
        if (::device.isInitialized && (thermometerListeners.isNotEmpty() || hygrometerListeners.isNotEmpty())) {
            // Wake up.
            device.tell(WAKE_UP)

            Thread.sleep(10)

            // Send a command to read register.
            device.tell(READ)

            Thread.sleep(1)

            // 2 bytes preamble, 4 bytes data, 2 bytes CRC.
            device.ask(8, this)
        }
    }

    override fun stop() {
        super.stop()

        if (::device.isInitialized) {
            device.removeListener(this)
        }
    }

    override fun onReceive(event: TwoWireEvent) {
        val data = event.data

        if (data[0] == READ_REGISTER_CMD) {
            humidity = ((data[2] shl 8) or data[3]) / 10.0
            hygrometerListeners.forEach { it.onHumidityChange(this) }

            temperature = ((data[4] and 0x7F shl 8) or data[5]) / 10.0
            if (data[4].bit7) temperature = -temperature
            thermometerListeners.forEach { it.onTemperatureChange(this) }
        } else if (data[0] == data[1] && data[0] == 0) {
            LOG.warn("cannot handle data. Is the sensor awake?")
        }
    }

    companion object {

        const val ADDRESS = 0x5C

        private const val READ_REGISTER_CMD = 0x03
        private const val REG_TEMP_H = 0x02
        private const val REG_HUM_H = 0x00

        private val LOG = LoggerFactory.getLogger(AM2320::class.java)

        @JvmStatic private val WAKE_UP = byteArrayOf(0)
        @JvmStatic private val READ = byteArrayOf(READ_REGISTER_CMD.toByte(), REG_HUM_H.toByte(), 4)
    }
}
