package br.tiagohm.kurubo.hardware

import br.tiagohm.kurubo.client.DaemonThreadFactory
import br.tiagohm.kurubo.client.Firmata
import br.tiagohm.kurubo.client.FirmataListener
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

abstract class AbstractHardware(firmata: Firmata) : Hardware {

    @Volatile private var poll: Future<*>? = null

    private val listener = object : FirmataListener {

        override fun onStart(firmata: Firmata) {
            start()
        }

        override fun onStop(firmata: Firmata) {
            stop()
        }
    }

    init {
        if (firmata.isReady) {
            SCHEDULED.schedule(::start, 1000L, TimeUnit.MILLISECONDS)
        } else {
            firmata.addListener(listener)
        }

        firmata.addListener(listener)
    }

    @Synchronized
    override fun start() {
        if (this is Pollable && pollInterval >= 1000L && poll == null) {
            val task = SCHEDULED.scheduleAtFixedRate(this, 1000L, pollInterval, TimeUnit.MILLISECONDS)
            poll = task
        }
    }

    override fun stop() {
        poll?.cancel(true)
        poll = null
    }

    companion object {

        private val DEFAULT_THREAD_FACTORY = DaemonThreadFactory("Hardware")
        private val SCHEDULED = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(), DEFAULT_THREAD_FACTORY)
    }
}
