package br.tiagohm.kurubo.hardware

import br.tiagohm.kurubo.client.Firmata
import br.tiagohm.kurubo.client.PinMode
import org.slf4j.LoggerFactory

data class Led(
    override val firmata: Firmata,
    val index: Int,
) : AbstractHardware(firmata), DigitalOutput {

    init {
        require(firmata.isPinDigital(index)) { "pin $index is not a digital pin" }
    }

    override val name = "LED $index"

    override val pin by lazy { firmata.pinAt(index) }

    override fun start() {
        if (pin.supports(PinMode.OUTPUT)) {
            pin.mode = PinMode.OUTPUT
        } else {
            LOG.error("pin $index does not support OUTPUT mode")
        }
    }

    companion object {

        private val LOG = LoggerFactory.getLogger(Led::class.java)
    }
}
