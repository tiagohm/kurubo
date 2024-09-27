package br.tiagohm.kurubo.hardware

import br.tiagohm.kurubo.client.Pin

interface AnalogInput : Hardware {

    val pin: Pin

    val value
        get() = pin.value

    fun addAnalogInputListener(listener: AnalogInputListener)

    fun removeAnalogInputListener(listener: AnalogInputListener)
}
