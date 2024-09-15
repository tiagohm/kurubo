package br.tiagohm.kurubo.hardware

fun interface DigitalInputListener {

    fun onPinStateChange(input: DigitalInput)
}
