package br.tiagohm.kurubo.client.protocol.fsm.state

import br.tiagohm.kurubo.client.protocol.END_SYSEX
import br.tiagohm.kurubo.client.protocol.fsm.FiniteStateMachine
import br.tiagohm.kurubo.client.protocol.fsm.event.AnalogMessageEvent

internal data class ParsingExtendedAnalogMessageState(override val finiteStateMachine: FiniteStateMachine) : AbstractState() {

    override fun process(b: Int) {
        if (b == END_SYSEX) {
            val portId = buf[0].toInt()
            var value = buf[1].toInt()

            for (i in 2 until count) {
                value = value or (buf[i].toInt() shl 7 * (i - 1))
            }

            publish(AnalogMessageEvent(portId, value))
            transitTo<WaitingForMessageState>()
        } else {
            write(b)
        }
    }
}
