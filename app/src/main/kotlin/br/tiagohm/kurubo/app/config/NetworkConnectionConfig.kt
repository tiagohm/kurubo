package br.tiagohm.kurubo.app.config

data class NetworkConnectionConfig(
    @JvmField val host: String = "",
    @JvmField val port: Int = 27016,
) {

    companion object {

        val EMPTY = NetworkConnectionConfig()
    }
}
