package br.tiagohm.kurubo.client.protocol.fsm.state

import br.tiagohm.kurubo.client.protocol.END_SYSEX
import br.tiagohm.kurubo.client.protocol.decode7Bit
import br.tiagohm.kurubo.client.protocol.fsm.FiniteStateMachine
import br.tiagohm.kurubo.client.protocol.fsm.event.TwoWireMessageEvent

internal data class ParsingTwoWireMessageState(override val finiteStateMachine: FiniteStateMachine) : AbstractState() {

    override fun process(b: Int) {
        if (b == END_SYSEX) {
            publish(decode())
            transitTo<WaitingForMessageState>()
        } else {
            write(b)
        }
    }

    private fun ParsingTwoWireMessageState.decode(): TwoWireMessageEvent {
        val address = decode7Bit(buf, 0)
        val register = decode7Bit(buf, 2)
        val data = IntArray((count - 4) / 2) { decode7Bit(buf, it * 2 + 4) }
        return TwoWireMessageEvent(address, register, data)
    }
}
