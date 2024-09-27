package br.tiagohm.kurubo.serial

import br.tiagohm.kurubo.transport.ByteArrayOutputStreamTransport
import br.tiagohm.kurubo.transport.TransportMode
import com.fazecast.jSerialComm.SerialPort
import com.fazecast.jSerialComm.SerialPortDataListener
import com.fazecast.jSerialComm.SerialPortEvent
import org.slf4j.LoggerFactory
import java.io.IOException

data class SerialTransport(
    private val portDescriptor: String,
    private val baudRate: Int = 115200,
    private val dataBits: Int = 8,
    private val stopBits: StopBits = StopBits.ONE,
    private val parity: Parity = Parity.NO,
) : ByteArrayOutputStreamTransport(1024) {

    private val serialPort = SerialPort.getCommPort(portDescriptor)
    private val queue = ByteBlockingQueue(32768)

    override val mode = TransportMode.SERIAL

    init {
        if (!serialPort.isOpen) {
            if (serialPort.openPort()) {
                serialPort.setComPortParameters(baudRate, dataBits, stopBits.value, parity.value)
                serialPort.addDataListener(object : SerialPortDataListener {

                    override fun serialEvent(event: SerialPortEvent) {
                        if (event.eventType == SerialPort.LISTENING_EVENT_DATA_RECEIVED) {
                            if (!queue.offer(event.receivedData)) {
                                LOG.warn("parser reached byte queue limit and some bytes were skipped")
                            }
                        }
                    }

                    override fun getListeningEvents(): Int {
                        return SerialPort.LISTENING_EVENT_DATA_RECEIVED
                    }
                })
            } else {
                throw IOException("cannot start firmata device: port=$serialPort")
            }
        }
    }

    override fun read(): Int {
        return queue.take().toInt() and 0xFF
    }

    @Synchronized
    override fun flush() {
        serialPort.writeBytes(buf, count, 0)
        reset()
    }

    override fun close() {
        if (serialPort.isOpen) {
            serialPort.closePort()
            queue.offer(-1)
        }
    }

    companion object {

        private val LOG = LoggerFactory.getLogger(SerialTransport::class.java)
    }
}
