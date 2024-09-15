package br.tiagohm.kurubo.client

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger
import kotlin.text.format

data class DaemonThreadFactory(private val name: String) : ThreadFactory {

    private val counter = AtomicInteger()

    override fun newThread(action: Runnable): Thread {
        val thread = Thread(action, "%s Thread %d".format(name, counter.incrementAndGet()))
        thread.isDaemon = true
        return thread
    }
}
