package br.tiagohm.kurubo.client.protocol.fsm.state

import br.tiagohm.kurubo.client.protocol.END_SYSEX
import br.tiagohm.kurubo.client.protocol.fsm.FiniteStateMachine
import br.tiagohm.kurubo.client.protocol.fsm.event.AnalogMappingEvent
import java.util.concurrent.ConcurrentHashMap

internal data class ParsingAnalogMappingState(override val finiteStateMachine: FiniteStateMachine) : AbstractState() {

    private val mapping = ConcurrentHashMap<Int, Int>(64)

    @Volatile private var portId = 0

    override fun process(b: Int) {
        if (b == END_SYSEX) {
            publish(AnalogMappingEvent(mapping))
            transitTo<WaitingForMessageState>()
        } else if (b != 127) {
            // If pin does support analog, corresponding analog id is in the byte.
            mapping[b] = portId
        }

        portId++
    }
}
