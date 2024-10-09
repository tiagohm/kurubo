package br.tiagohm.kurubo.app

import br.tiagohm.kurubo.app.config.Config
import br.tiagohm.kurubo.client.ArduinoUno
import br.tiagohm.kurubo.client.ESP8266
import br.tiagohm.kurubo.client.Firmata
import br.tiagohm.kurubo.hardware.*
import br.tiagohm.kurubo.hub.KuruboHub
import br.tiagohm.kurubo.network.NetworkTransport
import br.tiagohm.kurubo.serial.SerialTransport
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.github.rvesse.airline.annotations.Command
import com.github.rvesse.airline.annotations.Option
import java.nio.file.Path
import kotlin.io.path.inputStream

@Command(name = "kurubo")
class KuruboCommand : Runnable, AutoCloseable {

    @Option(name = ["-c", "--config"])
    private var config = "config.json"

    private val boards = ArrayList<Firmata>(4)

    @Volatile private var hub: KuruboHub? = null

    override fun run() {
        val config = Path.of(config).inputStream().use { MAPPER.readValue(it, Config::class.java) }

        hub = KuruboHub(config.webSocket.host, config.webSocket.port, MAPPER)

        for (b in config.boards) {
            val transport = when (b.connection.type) {
                ConnectionMode.SERIAL -> SerialTransport(b.connection.serial.portName, b.connection.serial.baudRate)
                ConnectionMode.NETWORK -> NetworkTransport(b.connection.network.host, b.connection.network.port)
            }

            val board = when (b.model) {
                BoardModel.ARDUINO_UNO -> ArduinoUno(transport)
                BoardModel.ESP8266 -> ESP8266(transport)
            }

            board.run()
            board.ensureInitializationIsDone()
            boards.add(board)

            for (h in b.hardwares) {
                val hardware = when (h.model) {
                    HardwareModel.LED -> Led(board, h.pin)
                    HardwareModel.BUTTON -> Button(board, h.pin, h.pullUp)
                    HardwareModel.LM35 -> LM35(board, h.pin, h.aref)
                    HardwareModel.AM2320 -> AM2320(board, pollInterval = DEFAULT_POLL_INTERVAL)
                    HardwareModel.BMP180 -> BMP180(board, pollInterval = DEFAULT_POLL_INTERVAL)
                    HardwareModel.BME280 -> BME280(board, BME280.ADDRESS_ALTERNATE or (h.address and 1), pollInterval = DEFAULT_POLL_INTERVAL / 2)
                }

                hub?.registerHardware(hardware)
            }
        }

        hub?.run()
    }

    override fun close() {
        hub?.stop()
        hub = null
    }

    companion object {

        private const val DEFAULT_POLL_INTERVAL = 1000L * 60

        private val MAPPER = jsonMapper {
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }
}
