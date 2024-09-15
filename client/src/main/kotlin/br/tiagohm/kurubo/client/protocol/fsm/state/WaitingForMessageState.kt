package br.tiagohm.kurubo.client.protocol.fsm.state

import br.tiagohm.kurubo.client.protocol.ANALOG_MESSAGE
import br.tiagohm.kurubo.client.protocol.DIGITAL_MESSAGE
import br.tiagohm.kurubo.client.protocol.REPORT_VERSION
import br.tiagohm.kurubo.client.protocol.START_SYSEX
import br.tiagohm.kurubo.client.protocol.SYSTEM_RESET
import br.tiagohm.kurubo.client.protocol.fsm.FiniteStateMachine
import br.tiagohm.kurubo.client.protocol.fsm.event.ErrorEvent
import br.tiagohm.kurubo.client.protocol.fsm.event.SystemResetEvent

internal data class WaitingForMessageState(override val finiteStateMachine: FiniteStateMachine) : AbstractState() {

    override fun process(b: Int) {
        // First byte may contain not only command but additional information as well.
        val command = if (b < 0xF0) (b and 0xF0) else b

        when (command) {
            DIGITAL_MESSAGE -> transitTo(ParsingDigitalMessageState(finiteStateMachine, b and 0x0F))
            ANALOG_MESSAGE -> transitTo(ParsingAnalogMessageState(finiteStateMachine, b and 0x0F))
            REPORT_VERSION -> transitTo<ParsingVersionMessageState>()
            START_SYSEX -> transitTo<ParsingSysexMessageState>()
            SYSTEM_RESET -> publish(SystemResetEvent)
            // Skip non control token.
            else -> publish(ErrorEvent(command))
        }
    }
}
