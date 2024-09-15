package br.tiagohm.kurubo.transport

import java.io.Flushable

interface Transport : Flushable, AutoCloseable {

    val mode: TransportMode

    fun read(): Int

    fun write(b: Int)

    fun write(data: ByteArray, offset: Int = 0, length: Int = data.size)

    companion object {

        @Suppress("NOTHING_TO_INLINE")
        inline fun Transport.sendValueAsTwo7bitBytes(value: Int) {
            write(value and 0x7F)
            write(value ushr 7 and 0x7F)
        }
    }
}
