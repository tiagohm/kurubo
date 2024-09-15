import br.tiagohm.kurubo.client.*
import br.tiagohm.kurubo.hardware.*
import br.tiagohm.kurubo.network.NetworkTransport
import br.tiagohm.kurubo.serial.SerialTransport
import kotlin.test.Test

@Suppress("unused")
class HardwareTest : FirmataListener, ThermometerListener, HygrometerListener, BarometerListener, AltimeterListener {

    @Test
    fun arduinoUno() {
        val transport = SerialTransport("/dev/ttyUSB0")
        val board = ArduinoUno(transport)
        board.addListener(this)

        val led = led(board, ArduinoUno.LED_BUILTIN)
        // am2320(board)
        // bmp180(board)
        // bme280(board)

        board.run()
        board.ensureInitializationIsDone()

        repeat(4) {
            led.toggle()
            Thread.sleep(5000)
        }

        board.close()
    }

    @Test
    fun esp8266() {
        val transport = NetworkTransport("192.168.31.137", 27016)
        val board = ESP8266(transport)
        board.addListener(this)

        val led = led(board, ESP8266.LED_BUILTIN)

        board.run()
        board.ensureInitializationIsDone()

        repeat(4) {
            led.toggle()
            Thread.sleep(5000)
        }

        board.close()
    }

    private fun led(firmata: Firmata, pin: Int): Led {
        return Led(firmata, pin)
    }

    private fun am2320(firmata: Firmata): AM2320 {
        val am2320 = AM2320(firmata)
        am2320.addThermometerListener(this)
        am2320.addHygrometerListener(this)
        return am2320
    }

    private fun bmp180(firmata: Firmata): BMP180 {
        val bmp180 = BMP180(firmata)
        bmp180.addThermometerListener(this)
        bmp180.addBarometerListener(this)
        bmp180.addAltimeterListener(this)
        return bmp180
    }

    private fun bme280(firmata: Firmata): BME280 {
        val bme280 = BME280(firmata, BME280.ADDRESS_ALTERNATE, BME280.Mode.FORCED)
        bme280.addThermometerListener(this)
        bme280.addBarometerListener(this)
        bme280.addAltimeterListener(this)
        bme280.addHygrometerListener(this)
        return bme280
    }

    private fun lm35(firmata: Firmata): LM35 {
        val lm35 = LM35(firmata, ArduinoUno.A0)
        lm35.addThermometerListener(this)
        return lm35
    }

    override fun onStart(firmata: Firmata) {
        println("started")
    }

    override fun onStop(firmata: Firmata) {
        println("stopped")
    }

    override fun onPinChange(firmata: Firmata, pin: Pin) {
        println("pin changed: $pin")
    }

    override fun onMessageReceive(firmata: Firmata, message: String) {
        println("message received: $message")
    }

    override fun onTemperatureChange(thermometer: Thermometer) {
        println("thermometer(${thermometer.name}): ${thermometer.temperature} °C")
    }

    override fun onHumidityChange(hygrometer: Hygrometer) {
        println("humidity(${hygrometer.name}): ${hygrometer.humidity} %")
    }

    override fun onPressureChange(barometer: Barometer) {
        println("pressure(${barometer.name}): ${barometer.pressure} Pa")
    }

    override fun onAltitudeChange(altimeter: Altimeter) {
        println("altitude(${altimeter.name}): ${altimeter.altitude} m")
    }
}
