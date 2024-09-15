package br.tiagohm.kurubo.serial

import com.fazecast.jSerialComm.SerialPort

enum class StopBits(@JvmField internal val value: Int) {
    ONE(SerialPort.ONE_STOP_BIT),
    ONE_POINT_FIVE(SerialPort.ONE_POINT_FIVE_STOP_BITS),
    TWO(SerialPort.TWO_STOP_BITS),
}
