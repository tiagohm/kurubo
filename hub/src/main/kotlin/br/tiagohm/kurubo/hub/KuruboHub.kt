package br.tiagohm.kurubo.hub

import br.tiagohm.kurubo.hardware.*
import com.fasterxml.jackson.databind.json.JsonMapper
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap

data class KuruboHub(
    private val address: InetSocketAddress,
    private val mapper: JsonMapper,
) : WebSocketServer(address), ThermometerListener, HygrometerListener, BarometerListener, AltimeterListener {

    constructor(host: String, port: Int, mapper: JsonMapper) : this(InetSocketAddress(host, port), mapper)

    private val hardwares = ConcurrentHashMap.newKeySet<Hardware>(64)

    @Volatile var connectionCount = 0
        private set

    val canBroadcast
        get() = connectionCount > 0

    fun registerHardware(hardware: Hardware) {
        if (hardwares.add(hardware)) {
            if (hardware is Thermometer) {
                hardware.addThermometerListener(this)
            }
            if (hardware is Hygrometer) {
                hardware.addHygrometerListener(this)
            }
            if (hardware is Barometer) {
                hardware.addBarometerListener(this)
            }
            if (hardware is Altimeter) {
                hardware.addAltimeterListener(this)
            }
        }
    }

    override fun onTemperatureChange(thermometer: Thermometer) {
        broadcast(HardwareData.thermometer(thermometer))
    }

    override fun onHumidityChange(hygrometer: Hygrometer) {
        broadcast(HardwareData.hygrometer(hygrometer))
    }

    override fun onPressureChange(barometer: Barometer) {
        broadcast(HardwareData.barometer(barometer))
    }

    override fun onAltitudeChange(altimeter: Altimeter) {
        broadcast(HardwareData.altimeter(altimeter))
    }

    fun broadcast() {
        if (canBroadcast) {
            for (hardware in hardwares) {
                if (hardware is Thermometer) {
                    onTemperatureChange(hardware)
                }
                if (hardware is Hygrometer) {
                    onHumidityChange(hardware)
                }
                if (hardware is Barometer) {
                    onPressureChange(hardware)
                }
                if (hardware is Altimeter) {
                    onAltitudeChange(hardware)
                }
            }
        }
    }

    private fun broadcast(data: HardwareData) {
        if (canBroadcast) {
            broadcast(mapper.writeValueAsString(data))
        }
    }

    override fun isDaemon(): Boolean {
        return true
    }

    override fun isReuseAddr(): Boolean {
        return true
    }

    override fun onStart() {
    }

    override fun onOpen(ws: WebSocket, handshake: ClientHandshake) {
        connectionCount++
        broadcast()
    }

    override fun onMessage(ws: WebSocket, message: String) = Unit

    override fun onError(ws: WebSocket?, e: Exception) = Unit

    override fun onClose(ws: WebSocket, code: Int, reason: String, remote: Boolean) {
        connectionCount--
    }
}
