package br.tiagohm.kurubo.client.protocol.fsm.state

import br.tiagohm.kurubo.client.protocol.fsm.FiniteStateMachine
import br.tiagohm.kurubo.client.protocol.fsm.event.DigitalMessageEvent

internal data class ParsingDigitalMessageState(
    override val finiteStateMachine: FiniteStateMachine,
    private val portId: Int,
) : AbstractState() {

    @Volatile private var counter = 0
    @Volatile private var value = 0

    override fun process(b: Int) {
        when (counter) {
            0 -> {
                value = b
                counter++
            }
            1 -> {
                value = value or (b shl 7)
                val pin = portId * 8

                repeat(8) {
                    publish(DigitalMessageEvent(pin + it, value ushr it and 0x01))
                }

                transitTo<WaitingForMessageState>()
            }
        }
    }
}
