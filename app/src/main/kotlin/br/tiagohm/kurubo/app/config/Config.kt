package br.tiagohm.kurubo.app.config

data class Config(
    @JvmField val webSocket: WebSocketConfig = WebSocketConfig.EMPTY,
    @JvmField val boards: List<BoardConfig> = emptyList(),
)
