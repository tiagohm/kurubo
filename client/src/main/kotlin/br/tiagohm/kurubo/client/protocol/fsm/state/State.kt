package br.tiagohm.kurubo.client.protocol.fsm.state

sealed interface State {

    fun process(b: Int)
}
