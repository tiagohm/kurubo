package br.tiagohm.kurubo.client

import br.tiagohm.kurubo.transport.Transport

@Suppress("unused")
class ESP8266(transport: Transport) : KuruboClient(transport) {

    override val numberOfDigitalPins = 17

    override val numberOfAnalogPins = 1

    override fun isPinLED(pin: Int) = pin == LED_BUILTIN || pin == LED_BUILTIN_AUX

    override fun isPinDigital(pin: Int) = pin in D3..D1 || pin in D6..<A0

    override fun isPinAnalog(pin: Int) = pin == A0

    override fun isPinPWM(pin: Int) = pin < A0

    override fun isPinServo(pin: Int) = isPinDigital(pin) && pin < MAX_SERVOS

    override fun isPinTwoWire(pin: Int) = pin == SDA || pin == SCL

    override fun isPinSPI(pin: Int) = pin == SS || pin == MOSI || pin == MISO || pin == SCK

    override fun pinToDigitalIndex(pin: Int) = pin

    override fun pinToAnalogIndex(pin: Int) = pin - A0

    override fun pinToPWMIndex(pin: Int) = pin

    override fun pinToServoIndex(pin: Int) = pin

    override fun toString() = "ESP8266"

    companion object {

        const val D0 = 16
        const val D1 = 5
        const val D2 = 4
        const val D3 = 0
        const val D4 = 2
        const val D5 = 14
        const val D6 = 12
        const val D7 = 13
        const val D8 = 15
        const val D9 = 3
        const val D10 = 1

        const val A0 = 17

        const val SDA = D2
        const val SCL = D1

        const val RX = D9
        const val TX = D10

        const val SS = D8
        const val MOSI = D7
        const val MISO = D6
        const val SCK = D5

        const val MAX_SERVOS = 9

        const val LED_BUILTIN = D4
        const val LED_BUILTIN_AUX = D0
    }
}
