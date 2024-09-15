package br.tiagohm.kurubo.hardware

interface Barometer : Altimeter {

    /**
     * Returns the pressure in Pascal.
     */
    val pressure: Double

    fun addBarometerListener(listener: BarometerListener)

    fun removeBarometerListener(listener: BarometerListener)
}
