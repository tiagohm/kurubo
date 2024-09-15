package br.tiagohm.kurubo.client

fun interface TwoWireListener {

    fun onReceive(event: TwoWireEvent)
}
