package br.tiagohm.kurubo.serial

import com.fazecast.jSerialComm.SerialPort

enum class Parity(@JvmField internal val value: Int) {
    NO(SerialPort.NO_PARITY),
    ODD(SerialPort.ODD_PARITY),
    EVEN(SerialPort.EVEN_PARITY),
    MARK(SerialPort.MARK_PARITY),
    SPACE(SerialPort.SPACE_PARITY),
}
