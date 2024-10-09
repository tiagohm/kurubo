package br.tiagohm.kurubo.hub

import br.tiagohm.kurubo.hub.HardwareType.*
import br.tiagohm.kurubo.hardware.Altimeter
import br.tiagohm.kurubo.hardware.Barometer
import br.tiagohm.kurubo.hardware.Hygrometer
import br.tiagohm.kurubo.hardware.Thermometer

internal data class HardwareData(
    @JvmField val type: HardwareType,
    @JvmField val name: String,
    @JvmField val value: Double,
) {

    companion object {

        fun thermometer(thermometer: Thermometer) = HardwareData(THERMOMETER, thermometer.name, thermometer.temperature)

        fun hygrometer(hygrometer: Hygrometer) = HardwareData(HYGROMETER, hygrometer.name, hygrometer.humidity)

        fun barometer(barometer: Barometer) = HardwareData(BAROMETER, barometer.name, barometer.pressure)

        fun altimeter(altimeter: Altimeter) = HardwareData(ALTIMETER, altimeter.name, altimeter.altitude)
    }
}
