package br.tiagohm.kurubo.hardware

import br.tiagohm.kurubo.client.Pinnable

interface AnalogInput : Hardware, Pinnable {

    val value
        get() = pin.value

    fun addAnalogInputListener(listener: AnalogInputListener)

    fun removeAnalogInputListener(listener: AnalogInputListener)
}
