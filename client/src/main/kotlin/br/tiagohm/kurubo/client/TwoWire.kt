package br.tiagohm.kurubo.client

import java.util.concurrent.TimeUnit

interface TwoWire {

    val address: Int

    fun delay(delay: Int)

    fun tell(data: ByteArray)

    fun ask(responseLength: Int, listener: TwoWireListener)

    fun ask(register: Int, responseLength: Int, listener: TwoWireListener)

    fun askSync(responseLength: Int, timeout: Long, unit: TimeUnit = TimeUnit.MILLISECONDS): TwoWireEvent?

    fun askSync(register: Int, responseLength: Int, timeout: Long, unit: TimeUnit = TimeUnit.MILLISECONDS): TwoWireEvent?

    fun addListener(listener: TwoWireListener)

    fun removeListener(listener: TwoWireListener)

    fun startReceivingUpdates(register: Int, messageLength: Int): Boolean

    fun startReceivingUpdates(messageLength: Int): Boolean

    fun stopReceivingUpdates()

    companion object {

        const val REGISTER_NOT_SET = -1
    }
}
