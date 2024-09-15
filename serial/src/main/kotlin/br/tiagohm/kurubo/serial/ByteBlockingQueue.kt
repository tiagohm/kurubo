package br.tiagohm.kurubo.serial

import java.util.concurrent.locks.ReentrantLock

class ByteBlockingQueue(private val buffer: ByteArray) {

    constructor(capacity: Int) : this(ByteArray(capacity))

    init {
        require(buffer.isNotEmpty()) { "invalid size: ${buffer.size}" }
    }

    @Volatile private var head = 0
    @Volatile private var tail = 0
    @Volatile private var count = 0

    private val lock = ReentrantLock()
    private val notEmpty = lock.newCondition()

    fun offer(b: Byte): Boolean {
        lock.lock()

        try {
            if (count >= buffer.size) {
                return false
            }

            buffer[tail] = b
            tail = (tail + 1) % buffer.size
            count++
            notEmpty.signal()
        } finally {
            lock.unlock()
        }

        return true
    }

    fun take(): Byte {
        lock.lock()

        try {
            while (count == 0) {
                notEmpty.await()
            }

            val b = buffer[head]
            head = (head + 1) % buffer.size
            count--
            return b
        } finally {
            lock.unlock()
        }
    }
}
