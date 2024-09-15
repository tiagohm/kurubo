package br.tiagohm.kurubo.client.protocol.fsm.parser

import br.tiagohm.kurubo.client.protocol.fsm.FiniteStateMachine
import br.tiagohm.kurubo.transport.Transport
import org.slf4j.LoggerFactory

internal data class FirmataParser(
    private val finiteStateMachine: FiniteStateMachine,
    private val transport: Transport,
) : Thread("Firmata Parser Thread") {

    init {
        isDaemon = true
    }

    override fun run() {
        while (true) {
            try {
                val byte = transport.read()

                if (byte >= 0) {
                    finiteStateMachine.process(byte)
                } else {
                    LOG.info("end of the stream has been reached")
                    break
                }
            } catch (_: InterruptedException) {
                break
            } catch (e: Throwable) {
                LOG.error("failed to process received data", e)
                break
            }
        }
    }

    companion object {

        private val LOG = LoggerFactory.getLogger(FirmataParser::class.java)
    }
}
