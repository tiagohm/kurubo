package br.tiagohm.kurubo.app.config

import br.tiagohm.kurubo.app.BoardModel

data class BoardConfig(
    @JvmField val name: String = "",
    @JvmField val model: BoardModel = BoardModel.ARDUINO_UNO,
    @JvmField val connection: ConnectionConfig = ConnectionConfig.EMPTY,
    @JvmField val hardwares: List<HardwareConfig> = emptyList(),
)
