package br.tiagohm.kurubo.hardware

fun interface HygrometerListener {

    fun onHumidityChange(hygrometer: Hygrometer)
}
