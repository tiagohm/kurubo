package br.tiagohm.kurubo.hardware

import br.tiagohm.kurubo.client.Firmata
import br.tiagohm.kurubo.client.FirmataListener
import br.tiagohm.kurubo.client.Pin
import br.tiagohm.kurubo.client.PinMode
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

data class Button(
    override val firmata: Firmata,
    val index: Int,
    val pullUp: Boolean = false,
) : AbstractHardware(firmata), DigitalInput {

    private val listeners = ConcurrentHashMap.newKeySet<DigitalInputListener>(1)

    override val name = "Button $index"

    override val pin by lazy { firmata.pinAt(index) }

    private val pinChanged = object : FirmataListener {

        override fun onPinChange(firmata: Firmata, pin: Pin) {
            if (pin === this@Button.pin) {
                listeners.forEach { it.onPinStateChange(this@Button) }
            }
        }
    }

    override fun addDigitalInputListener(listener: DigitalInputListener) {
        listeners.add(listener)
    }

    override fun removeDigitalInputListener(listener: DigitalInputListener) {
        listeners.remove(listener)
    }

    override fun start() {
        super.start()

        val mode = if (pullUp) PinMode.PULL_UP else PinMode.INPUT

        if (pin.supports(mode)) {
            firmata.addListener(pinChanged)
            firmata.digitalPinReport(pin.index, true)
        } else {
            LOG.error("pin $index does not support $mode mode")
        }
    }

    override fun stop() {
        super.stop()
        firmata.digitalPinReport(pin.index, false)
        firmata.removeListener(pinChanged)
    }

    companion object {

        private val LOG = LoggerFactory.getLogger(Button::class.java)
    }
}
