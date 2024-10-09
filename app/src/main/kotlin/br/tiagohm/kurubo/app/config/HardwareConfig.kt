package br.tiagohm.kurubo.app.config

import br.tiagohm.kurubo.app.HardwareModel

data class HardwareConfig(
    @JvmField val model: HardwareModel = HardwareModel.LED,
    @JvmField val pin: Int = 0,
    @JvmField val address: Int = 0,
    @JvmField val pullUp: Boolean = false,
    @JvmField val aref: Double = 5.0,
)
