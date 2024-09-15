package br.tiagohm.kurubo.client.protocol.fsm.state

import br.tiagohm.kurubo.client.protocol.fsm.FiniteStateMachine
import br.tiagohm.kurubo.client.protocol.fsm.event.VersionMessageEvent

internal data class ParsingVersionMessageState(override val finiteStateMachine: FiniteStateMachine) : AbstractState() {

    @Volatile private var counter = 0
    @Volatile private var major = 0

    override fun process(b: Int) {
        if (counter == 0) {
            major = b
            counter++
        } else {
            publish(VersionMessageEvent(major, b))
            transitTo<WaitingForMessageState>()
        }
    }
}
