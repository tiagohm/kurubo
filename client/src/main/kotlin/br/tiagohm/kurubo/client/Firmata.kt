package br.tiagohm.kurubo.client

import br.tiagohm.kurubo.client.protocol.fsm.event.Event
import java.util.function.Consumer

interface Firmata : Runnable, AutoCloseable {

    val isReady: Boolean

    fun addListener(listener: FirmataListener)

    fun removeListener(listener: FirmataListener)

    fun ensureInitializationIsDone()

    val pins: Collection<Pin>

    val pinCount: Int

    val numberOfDigitalPins: Int

    val numberOfAnalogPins: Int

    fun isPinLED(pin: Int): Boolean

    fun isPinDigital(pin: Int): Boolean

    fun isPinAnalog(pin: Int): Boolean

    fun isPinPWM(pin: Int): Boolean

    fun isPinServo(pin: Int): Boolean

    fun isPinTwoWire(pin: Int): Boolean

    fun isPinSPI(pin: Int): Boolean

    fun pinToDigitalIndex(pin: Int): Int

    fun pinToAnalogIndex(pin: Int): Int

    fun pinToPWMIndex(pin: Int): Int

    fun pinToServoIndex(pin: Int): Int

    fun pinAt(index: Int): Pin

    fun <T : Event> addHandler(type: Class<out T>, handler: Consumer<in T>)

    fun analogMapping()

    fun analogWrite(pin: Int, value: Int)

    fun capability()

    fun digitalWrite(portId: Int, value: Int)

    fun requestFirmware()

    fun pinState(pin: Int)

    fun analogPinReport(pin: Int, enabled: Boolean)

    fun analogReport(enabled: Boolean)

    fun digitalPinReport(pin: Int, enabled: Boolean)

    fun digitalReport(enabled: Boolean)

    fun samplingInterval(interval: Int)

    fun servoConfig(pin: Int, minPulse: Int, maxPulse: Int)

    fun mode(pin: Int, mode: PinMode)

    fun text(message: String)

    fun twoWireDelay(delay: Int)

    fun twoWireDevice(address: Int): TwoWire

    fun twoWireConfig(delayInMicroseconds: Int)

    fun twoWireRead(slaveAddress: Int, register: Int, bytesToRead: Int, continuous: Boolean)

    fun twoWireStopContinuous(slaveAddress: Int)

    fun twoWireWrite(slaveAddress: Int, bytesToWrite: ByteArray)
}
