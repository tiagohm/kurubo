package br.tiagohm.kurubo.hardware

import br.tiagohm.kurubo.client.Pinnable

interface DigitalInput : Hardware, Pinnable {

    val value
        get() = pin.value != 0

    fun addDigitalInputListener(listener: DigitalInputListener)

    fun removeDigitalInputListener(listener: DigitalInputListener)
}
