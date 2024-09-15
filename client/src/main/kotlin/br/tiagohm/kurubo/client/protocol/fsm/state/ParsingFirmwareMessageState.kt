package br.tiagohm.kurubo.client.protocol.fsm.state

import br.tiagohm.kurubo.client.protocol.END_SYSEX
import br.tiagohm.kurubo.client.protocol.fsm.FiniteStateMachine
import br.tiagohm.kurubo.client.protocol.fsm.event.FirmwareMessageEvent

internal data class ParsingFirmwareMessageState(override val finiteStateMachine: FiniteStateMachine) : AbstractState() {

    override fun process(b: Int) {
        if (b == END_SYSEX) {
            val major = buf[0].toInt()
            val minor = buf[1].toInt()
            val name = ParsingStringMessageState.decode(buf, 2, count - 2)
            publish(FirmwareMessageEvent(major, minor, name))
            transitTo<WaitingForMessageState>()
        } else {
            write(b)
        }
    }
}
