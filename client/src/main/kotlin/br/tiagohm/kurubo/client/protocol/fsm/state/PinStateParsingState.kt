package br.tiagohm.kurubo.client.protocol.fsm.state

import br.tiagohm.kurubo.client.PinMode
import br.tiagohm.kurubo.client.protocol.END_SYSEX
import br.tiagohm.kurubo.client.protocol.fsm.FiniteStateMachine
import br.tiagohm.kurubo.client.protocol.fsm.event.PinStateEvent

internal data class PinStateParsingState(override val finiteStateMachine: FiniteStateMachine) : AbstractState() {

    override fun process(b: Int) {
        if (b == END_SYSEX) {
            var value = 0

            for (i in 2 until count) {
                value = value or (buf[i].toInt() shl ((i - 2) * 7))
            }

            publish(PinStateEvent(buf[0].toInt(), PinMode.resolve(buf[1].toInt()), value))
            transitTo<WaitingForMessageState>()
        } else {
            write(b)
        }
    }
}
