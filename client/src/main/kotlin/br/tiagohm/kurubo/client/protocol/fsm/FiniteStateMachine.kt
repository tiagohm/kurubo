package br.tiagohm.kurubo.client.protocol.fsm

import br.tiagohm.kurubo.client.protocol.fsm.event.Event
import br.tiagohm.kurubo.client.protocol.fsm.event.FiniteStateMachineInTerminalStateEvent
import br.tiagohm.kurubo.client.protocol.fsm.state.State
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.function.Consumer

internal class FiniteStateMachine(
    initialState: Class<out State>,
    private val executor: Executor,
) {

    private val handlers = ConcurrentHashMap<Class<*>, Consumer<in Event>>()

    @Volatile private var currentState = initialState.getConstructor(FiniteStateMachine::class.java).newInstance(this)

    fun transitTo(state: State) {
        currentState = state
    }

    fun transitTo(type: Class<out State>) {
        try {
            val state = type.getConstructor(FiniteStateMachine::class.java).newInstance(this)
            transitTo(state)
        } catch (e: ReflectiveOperationException) {
            throw IllegalArgumentException("cannot instantiate the new state from type: $type", e)
        }
    }

    fun handle(event: Event) {
        val handler = handlers[event::class.java]

        if (handler == null) {
            LOG.warn("no specific event handler is registered for {}", event)
        } else {
            LOG.debug("received: {}", event)
            executor.execute { handler.accept(event) }
        }

        executor.execute { handlers[Any::class.java]?.accept(event) }
    }

    fun process(b: Int) {
        if (currentState == null) {
            handle(FiniteStateMachineInTerminalStateEvent)
        } else {
            currentState!!.process(b)
        }
    }

    fun process(buffer: ByteArray, offset: Int = 0, length: Int = buffer.size) {
        for (i in offset until offset + length) {
            process(buffer[i].toInt() and 0xFF)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Event> addHandler(type: Class<out T>, handler: Consumer<in T>) {
        if (handlers.containsKey(type)) {
            handlers[type] = handlers[type]!!.andThen(handler as Consumer<Any?>)
        } else {
            handlers[type] = handler as Consumer<in Event>
        }
    }

    inline fun <reified T : Event> addHandler(handler: Consumer<in T>) {
        addHandler(T::class.java, handler)
    }

    companion object {

        private val LOG = LoggerFactory.getLogger(FiniteStateMachine::class.java)
    }
}
