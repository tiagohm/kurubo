package br.tiagohm.kurubo.client

import br.tiagohm.kurubo.client.TwoWire.Companion.REGISTER_NOT_SET
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

internal data class TwoWireDevice(
    private val master: Firmata,
    override val address: Int,
) : TwoWire {

    private val receivingUpdates = AtomicBoolean()
    private val callbacks = ConcurrentHashMap<Int, TwoWireListener>(1)
    private val subscribers = ConcurrentHashMap.newKeySet<TwoWireListener>(1)

    override fun delay(delay: Int) {
        master.twoWireDelay(delay)
    }

    override fun tell(data: ByteArray) {
        master.twoWireWrite(address, data)
    }

    override fun ask(responseLength: Int, listener: TwoWireListener) {
        ask(REGISTER_NOT_SET, responseLength, listener)
    }

    override fun ask(register: Int, responseLength: Int, listener: TwoWireListener) {
        callbacks[register] = listener
        master.twoWireRead(address, register, responseLength, false)
    }

    override fun askSync(responseLength: Int, timeout: Long, unit: TimeUnit): TwoWireEvent? {
        return askSync(REGISTER_NOT_SET, responseLength, timeout)
    }

    override fun askSync(register: Int, responseLength: Int, timeout: Long, unit: TimeUnit): TwoWireEvent? {
        return try {
            val completable = CompletableFuture<TwoWireEvent>()
            ask(register, responseLength, completable::complete)
            return completable.get(timeout, unit)
        } catch (_: Throwable) {
            null
        }
    }

    override fun addListener(listener: TwoWireListener) {
        subscribers.add(listener)
    }

    override fun removeListener(listener: TwoWireListener) {
        subscribers.remove(listener)
    }

    override fun startReceivingUpdates(register: Int, messageLength: Int): Boolean {
        val result = receivingUpdates.compareAndSet(false, true)

        if (result) {
            master.twoWireRead(address, register, messageLength, true)
        }

        return result
    }

    override fun startReceivingUpdates(messageLength: Int): Boolean {
        val result = receivingUpdates.compareAndSet(false, true)

        if (result) {
            master.twoWireRead(address, REGISTER_NOT_SET, messageLength, true)
        }

        return result
    }

    override fun stopReceivingUpdates() {
        if (receivingUpdates.compareAndSet(true, false)) {
            master.twoWireStopContinuous(address)
        }
    }

    internal fun onReceive(register: Int, message: IntArray) {
        val event = TwoWireEvent(this, register, message)
        val listener = callbacks.remove(register)

        if (listener == null) {
            subscribers.forEach { it.onReceive(event) }
        } else {
            listener.onReceive(event)
        }
    }

    override fun toString() = "TwoWireDevice(address=$address)"
}
