package br.tiagohm.kurubo.network

import br.tiagohm.kurubo.transport.Transport
import br.tiagohm.kurubo.transport.TransportMode
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

data class NetworkTransport(val address: InetSocketAddress) : Transport {

    private val socket = Socket()

    constructor(host: String, port: Int = 27016) : this(InetSocketAddress(host, port))

    private val inputStream: InputStream
    private val outputStream: OutputStream

    init {
        socket.keepAlive = true
        socket.connect(address)
        inputStream = socket.getInputStream()
        outputStream = socket.getOutputStream()
    }

    override val mode = TransportMode.NETWORK

    override fun read(): Int {
        return inputStream.read()
    }

    override fun write(b: Int) {
        outputStream.write(b)
    }

    override fun write(data: ByteArray, offset: Int, length: Int) {
        outputStream.write(data, offset, length)
    }

    override fun flush() {
        outputStream.flush()
    }

    override fun close() {
        socket.close()
    }
}
