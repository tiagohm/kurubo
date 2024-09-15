package br.tiagohm.kurubo.hardware

fun interface AnalogInputListener {

    fun onPinValueChange(input: AnalogInput)
}
