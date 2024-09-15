package br.tiagohm.kurubo.hardware

fun interface BarometerListener {

    fun onPressureChange(barometer: Barometer)
}
