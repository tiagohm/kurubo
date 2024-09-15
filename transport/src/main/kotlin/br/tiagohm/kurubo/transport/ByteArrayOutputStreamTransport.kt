package br.tiagohm.kurubo.transport

import java.io.ByteArrayOutputStream

abstract class ByteArrayOutputStreamTransport(size: Int = 1024) : ByteArrayOutputStream(size), Transport
