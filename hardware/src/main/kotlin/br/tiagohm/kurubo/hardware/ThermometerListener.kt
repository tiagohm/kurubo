package br.tiagohm.kurubo.hardware

fun interface ThermometerListener {

    fun onTemperatureChange(thermometer: Thermometer)
}
