package br.tiagohm.kurubo.app.config

data class SerialConnectionConfig(
    @JvmField val portName: String = "",
    @JvmField val baudRate: Int = 115200,
) {

    companion object {

        val EMPTY = SerialConnectionConfig()
    }
}
