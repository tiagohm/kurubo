package br.tiagohm.kurubo.client.protocol.fsm.state

import br.tiagohm.kurubo.client.protocol.END_SYSEX
import br.tiagohm.kurubo.client.protocol.fsm.FiniteStateMachine
import br.tiagohm.kurubo.client.protocol.fsm.event.CustomMessageEvent

internal data class ParsingCustomSysexMessageState(override val finiteStateMachine: FiniteStateMachine) : AbstractState() {

    override fun process(b: Int) {
        if (b == END_SYSEX) {
            publish(CustomMessageEvent(buf, count))
            transitTo<WaitingForMessageState>()
        } else {
            write(b)
        }
    }
}
