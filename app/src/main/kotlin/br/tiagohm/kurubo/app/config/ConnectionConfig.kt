package br.tiagohm.kurubo.app.config

import br.tiagohm.kurubo.app.ConnectionMode

data class ConnectionConfig(
    @JvmField val type: ConnectionMode = ConnectionMode.SERIAL,
    @JvmField val serial: SerialConnectionConfig = SerialConnectionConfig.EMPTY,
    @JvmField val network: NetworkConnectionConfig = NetworkConnectionConfig.EMPTY,
) {

    companion object {

        val EMPTY = ConnectionConfig()
    }
}
