package br.tiagohm.kurubo.client.protocol.fsm.state

import br.tiagohm.kurubo.client.protocol.fsm.FiniteStateMachine
import br.tiagohm.kurubo.client.protocol.fsm.event.Event
import java.io.ByteArrayOutputStream

internal sealed class AbstractState : State, ByteArrayOutputStream() {

    protected abstract val finiteStateMachine: FiniteStateMachine

    protected fun transitTo(type: Class<out State>) {
        finiteStateMachine.transitTo(type)
    }

    protected inline fun <reified T : State> transitTo() {
        finiteStateMachine.transitTo(T::class.java)
    }

    protected fun transitTo(state: State) {
        finiteStateMachine.transitTo(state)
    }

    protected fun publish(event: Event) {
        finiteStateMachine.handle(event)
    }
}
