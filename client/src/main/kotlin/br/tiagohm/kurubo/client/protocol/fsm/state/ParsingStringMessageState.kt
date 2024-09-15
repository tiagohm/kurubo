package br.tiagohm.kurubo.client.protocol.fsm.state

import br.tiagohm.kurubo.client.protocol.END_SYSEX
import br.tiagohm.kurubo.client.protocol.decode7Bit
import br.tiagohm.kurubo.client.protocol.fsm.FiniteStateMachine
import br.tiagohm.kurubo.client.protocol.fsm.event.StringMessageEvent

internal data class ParsingStringMessageState(override val finiteStateMachine: FiniteStateMachine) : AbstractState() {

    override fun process(b: Int) {
        if (b == END_SYSEX) {
            val message = decode(buf, length = count)
            publish(StringMessageEvent(message))
            transitTo<WaitingForMessageState>()
        } else {
            write(b)
        }
    }

    companion object {

        internal fun decode(buffer: ByteArray, offset: Int = 0, length: Int = buffer.size): String {
            return CharArray(length / 2) { decode7Bit(buffer, offset + it * 2).toChar() }.concatToString()
        }
    }
}
