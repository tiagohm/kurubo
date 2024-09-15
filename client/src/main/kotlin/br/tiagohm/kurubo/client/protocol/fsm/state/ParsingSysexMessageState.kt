package br.tiagohm.kurubo.client.protocol.fsm.state

import br.tiagohm.kurubo.client.protocol.ANALOG_MAPPING_RESPONSE
import br.tiagohm.kurubo.client.protocol.CAPABILITY_RESPONSE
import br.tiagohm.kurubo.client.protocol.EXTENDED_ANALOG
import br.tiagohm.kurubo.client.protocol.PIN_STATE_RESPONSE
import br.tiagohm.kurubo.client.protocol.REPORT_FIRMWARE
import br.tiagohm.kurubo.client.protocol.STRING_DATA
import br.tiagohm.kurubo.client.protocol.TWO_WIRE_REPLY
import br.tiagohm.kurubo.client.protocol.fsm.FiniteStateMachine

internal data class ParsingSysexMessageState(override val finiteStateMachine: FiniteStateMachine) : AbstractState() {

    override fun process(b: Int) {
        val nextState = STATES[b]

        if (nextState == null) {
            val newState = ParsingCustomSysexMessageState(finiteStateMachine)
            newState.process(b)
            transitTo(newState)
        } else {
            transitTo(nextState)
        }
    }

    companion object {

        private val STATES = mapOf(
            REPORT_FIRMWARE to ParsingFirmwareMessageState::class.java,
            EXTENDED_ANALOG to ParsingExtendedAnalogMessageState::class.java,
            CAPABILITY_RESPONSE to ParsingCapabilityResponseState::class.java,
            ANALOG_MAPPING_RESPONSE to ParsingAnalogMappingState::class.java,
            PIN_STATE_RESPONSE to PinStateParsingState::class.java,
            STRING_DATA to ParsingStringMessageState::class.java,
            TWO_WIRE_REPLY to ParsingTwoWireMessageState::class.java,
        )
    }
}
