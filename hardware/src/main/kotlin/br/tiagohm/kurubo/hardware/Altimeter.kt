package br.tiagohm.kurubo.hardware

import kotlin.math.pow

interface Altimeter : Hardware {

    /**
     * Returns the altitude in meters.
     */
    val altitude: Double

    fun addAltimeterListener(listener: AltimeterListener)

    fun removeAltimeterListener(listener: AltimeterListener)

    companion object {

        @Suppress("NOTHING_TO_INLINE")
        inline fun estimatedAltitudeFromPressureAndTemperature(pressure: Double, temperature: Double = 15.0): Double {
            return ((temperature + 273.15) / 0.0065) * (1.0 - (pressure / 101325).pow(1.0 / 5.255))
        }
    }
}
