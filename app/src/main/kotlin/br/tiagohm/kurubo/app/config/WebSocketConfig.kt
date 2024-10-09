package br.tiagohm.kurubo.app.config

data class WebSocketConfig(
    @JvmField val host: String = "0.0.0.0",
    @JvmField val port: Int = 9090,
) {

    companion object {

        val EMPTY = WebSocketConfig()
    }
}
