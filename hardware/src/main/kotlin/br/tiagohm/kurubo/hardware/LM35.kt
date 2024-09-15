package br.tiagohm.kurubo.hardware

import br.tiagohm.kurubo.client.Firmata
import br.tiagohm.kurubo.client.FirmataListener
import br.tiagohm.kurubo.client.Pin
import br.tiagohm.kurubo.client.PinMode
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

data class LM35(
    override val firmata: Firmata,
    val index: Int,
    val aref: Double = 5.0,
) : AbstractHardware(firmata), AnalogInput, Thermometer {

    init {
        require(firmata.isPinAnalog(index)) { "pin $index is not an analog pin" }
    }

    override val name = "LM35"

    @Volatile override var temperature = 0.0
        private set

    override val pin by lazy { firmata.pinAt(index) }

    private val thermometerListeners = ConcurrentHashMap.newKeySet<ThermometerListener>(1)
    private val analogInputListeners = ConcurrentHashMap.newKeySet<AnalogInputListener>(1)

    private val pinChanged = object : FirmataListener {

        override fun onPinChange(firmata: Firmata, pin: Pin) {
            if (pin === this@LM35.pin) {
                analogInputListeners.forEach { it.onPinValueChange(this@LM35) }

                if (thermometerListeners.isNotEmpty()) {
                    temperature = aref * 1000.0 * pin.value / 10230.0
                    thermometerListeners.forEach { it.onTemperatureChange(this@LM35) }
                }
            }
        }
    }

    override fun addThermometerListener(listener: ThermometerListener) {
        thermometerListeners.add(listener)
    }

    override fun removeThermometerListener(listener: ThermometerListener) {
        thermometerListeners.remove(listener)
    }

    override fun addAnalogInputListener(listener: AnalogInputListener) {
        analogInputListeners.add(listener)
    }

    override fun removeAnalogInputListener(listener: AnalogInputListener) {
        analogInputListeners.remove(listener)
    }

    override fun start() {
        super.start()

        if (pin.supports(PinMode.ANALOG)) {
            firmata.addListener(pinChanged)
            firmata.analogPinReport(pin.index, true)
        } else {
            LOG.error("pin $index does not support ANALOG mode")
        }
    }

    override fun stop() {
        super.stop()

        firmata.analogPinReport(pin.index, false)
        firmata.removeListener(pinChanged)
    }

    companion object {

        private val LOG = LoggerFactory.getLogger(LM35::class.java)
    }
}
