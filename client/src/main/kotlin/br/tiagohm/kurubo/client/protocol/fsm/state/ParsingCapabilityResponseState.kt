package br.tiagohm.kurubo.client.protocol.fsm.state

import br.tiagohm.kurubo.client.PinMode
import br.tiagohm.kurubo.client.protocol.END_SYSEX
import br.tiagohm.kurubo.client.protocol.fsm.FiniteStateMachine
import br.tiagohm.kurubo.client.protocol.fsm.event.PinCapabilitiesFinishedEvent
import br.tiagohm.kurubo.client.protocol.fsm.event.PinCapabilityResponseEvent

internal data class ParsingCapabilityResponseState @JvmOverloads constructor(
    override val finiteStateMachine: FiniteStateMachine,
    private val pin: Int = 0,
) : AbstractState() {

    override fun process(b: Int) {
        when (b) {
            END_SYSEX -> {
                publish(PinCapabilitiesFinishedEvent)
                transitTo<WaitingForMessageState>()
            }
            127 -> {
                val supportedModes = HashSet<PinMode>(count / 2)
                var i = 0

                while (i < count) {
                    // Every second byte contains mode's resolution of pin.
                    supportedModes.add(PinMode.resolve(buf[i].toInt()))
                    i += 2
                }

                publish(PinCapabilityResponseEvent(pin, supportedModes))
                transitTo(ParsingCapabilityResponseState(finiteStateMachine, pin + 1))
            }
            else -> {
                write(b)
            }
        }
    }
}
