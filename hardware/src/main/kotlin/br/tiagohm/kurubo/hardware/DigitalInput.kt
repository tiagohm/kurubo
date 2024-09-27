package br.tiagohm.kurubo.hardware

import br.tiagohm.kurubo.client.Pin

interface DigitalInput : Hardware {

    val pin: Pin

    val value
        get() = pin.value != 0

    fun addDigitalInputListener(listener: DigitalInputListener)

    fun removeDigitalInputListener(listener: DigitalInputListener)
}
