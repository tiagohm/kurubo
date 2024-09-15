package br.tiagohm.kurubo.hardware

interface Hygrometer : Hardware {

    /**
     * Returns the relative humidity in percentage.
     */
    val humidity: Double

    fun addHygrometerListener(listener: HygrometerListener)

    fun removeHygrometerListener(listener: HygrometerListener)
}
