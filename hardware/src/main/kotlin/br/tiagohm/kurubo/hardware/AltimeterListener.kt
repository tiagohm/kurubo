package br.tiagohm.kurubo.hardware

fun interface AltimeterListener {

    fun onAltitudeChange(altimeter: Altimeter)
}
