package br.tiagohm.kurubo.network

import br.tiagohm.kurubo.transport.Transport
import br.tiagohm.kurubo.transport.TransportMode
import java.net.InetSocketAddress
import java.net.Socket

data class NetworkTransport(val address: InetSocketAddress) : Transport {

    private val socket = Socket()

    constructor(host: String, port: Int = 27016) : this(InetSocketAddress(host, port))

    init {
        socket.keepAlive = true
        socket.connect(address)
    }

    override val mode = TransportMode.NETWORK

    override fun read(): Int {
        return socket.getInputStream().read()
    }

    override fun write(b: Int) {
        socket.getOutputStream().write(b)
    }

    override fun write(data: ByteArray, offset: Int, length: Int) {
        socket.getOutputStream().write(data, offset, length)
    }

    override fun flush() {
        socket.getOutputStream().flush()
    }

    override fun close() {
        socket.close()
    }
}
