package br.tiagohm.kurubo.hardware

interface Thermometer : Hardware {

    /**
     * Returns the temperature in Celsius.
     */
    val temperature: Double

    fun addThermometerListener(listener: ThermometerListener)

    fun removeThermometerListener(listener: ThermometerListener)
}
