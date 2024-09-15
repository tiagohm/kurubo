package br.tiagohm.kurubo.client

import br.tiagohm.kurubo.client.TwoWire.Companion.REGISTER_NOT_SET
import br.tiagohm.kurubo.client.protocol.*
import br.tiagohm.kurubo.client.protocol.fsm.FiniteStateMachine
import br.tiagohm.kurubo.client.protocol.fsm.event.*
import br.tiagohm.kurubo.client.protocol.fsm.parser.FirmataParser
import br.tiagohm.kurubo.client.protocol.fsm.state.WaitingForMessageState
import br.tiagohm.kurubo.transport.Transport
import br.tiagohm.kurubo.transport.Transport.Companion.sendValueAsTwo7bitBytes
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import kotlin.math.max
import kotlin.math.min

abstract class KuruboClient(private val transport: Transport) : Firmata {

    private val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), DEFAULT_THREAD_FACTORY)
    private val protocol = FiniteStateMachine(WaitingForMessageState::class.java, executor)
    private val parser = FirmataParser(protocol, transport)

    private val listeners = ConcurrentHashMap.newKeySet<FirmataListener>(1)
    private val pinStateRequestQueue = ArrayDeque<Int>(64)
    private val started = AtomicBoolean()
    private val ready = AtomicBoolean()
    private val completable = CompletableFuture<Unit>()
    private val initializedPins = AtomicInteger(0)
    private val analogMapping = HashMap<Int, Int>(16)
    private val foundPins = HashMap<Int, FirmataPin>(32)

    private val longestTwoWireDelay = AtomicInteger(0)
    private val twoWireDevices = ConcurrentHashMap<Int, TwoWireDevice>(8)

    init {
        protocol.addHandler<FiniteStateMachineInTerminalStateEvent> {
            LOG.error("parser has reached the terminal state. It may be due receiving of unsupported command.")
        }
    }

    override fun run() {
        if (started.compareAndSet(false, true)) {
            try {
                parser.start()
                requestFirmware()
            } catch (e: Throwable) {
                transport.close()
                parser.interrupt()
                throw e
            }
        }
    }

    final override fun addListener(listener: FirmataListener) {
        listeners.add(listener)
    }

    final override fun removeListener(listener: FirmataListener) {
        listeners.remove(listener)
    }

    final override val isReady
        get() = ready.get()

    final override fun ensureInitializationIsDone() {
        if (!started.get()) {
            try {
                run()
            } catch (e: IOException) {
                throw InterruptedException(e.message)
            }
        }

        try {
            if (!isReady) {
                completable.get(TIMEOUT, TimeUnit.MILLISECONDS)
            }
        } catch (e: Throwable) {
            throw InterruptedException(
                """
                    Connection timeout.
                    Please, make sure the board runs a firmware that supports Firmata protocol.
                    The firmware has to implement callbacks for CAPABILITY_QUERY, PIN_STATE_QUERY and ANALOG_MAPPING_QUERY in order for the initialization to work.
                    """.trimIndent()
            )
        }
    }

    final override val pins: Collection<Pin>
        get() = foundPins.values

    final override val pinCount
        get() = foundPins.size

    final override fun pinAt(index: Int): Pin {
        return foundPins[index]!!
    }

    final override fun twoWireDelay(delay: Int) {
        var longestDelaySoFar = longestTwoWireDelay.get()

        while (longestDelaySoFar < delay) {
            if (longestTwoWireDelay.compareAndSet(longestDelaySoFar, delay)) {
                twoWireConfig(delay)
            }

            longestDelaySoFar = longestTwoWireDelay.get()
        }
    }

    final override fun twoWireDevice(address: Int): TwoWire {
        if (!twoWireDevices.contains(address)) {
            twoWireDevices[address] = TwoWireDevice(this, address)
            twoWireConfig(longestTwoWireDelay.get())
        }

        return twoWireDevices[address]!!
    }

    final override fun <T : Event> addHandler(type: Class<out T>, handler: Consumer<in T>) {
        protocol.addHandler(type, handler)
    }

    inline fun <reified T : Event> addHandler(handler: Consumer<in T>) {
        addHandler(T::class.java, handler)
    }

    internal fun pinChanged(pin: Pin) {
        for (listener in listeners) {
            listener.onPinChange(this, pin)
        }
    }

    private fun shutdown() {
        executor.shutdown()

        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow()

                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    LOG.error("cannot stop an event handling executor. It may result in a thread leak.")
                }
            }
        } catch (_: InterruptedException) {
            executor.shutdownNow()
        }
    }

    override fun close() {
        analogReport(false)
        digitalReport(false)

        parser.interrupt()
        transport.close()

        shutdown()

        for (listener in listeners) {
            listener.onStop(this)
        }

        listeners.clear()
    }

    override fun analogMapping() {
        transport.write(START_SYSEX)
        transport.write(ANALOG_MAPPING_QUERY)
        transport.write(END_SYSEX)
        transport.flush()
    }

    override fun analogWrite(pin: Int, value: Int) {
        if (pin <= 15) {
            transport.write(ANALOG_MESSAGE or (pin and 0x0F))
            transport.sendValueAsTwo7bitBytes(value)
        } else {
            transport.write(START_SYSEX)
            transport.write(EXTENDED_ANALOG)
            transport.write(pin)
            transport.sendValueAsTwo7bitBytes(value)

            if (value > 0x00004000) {
                transport.write(value ushr 14 and 0x7F)
            }

            if (value > 0x00200000) {
                transport.write(value ushr 21 and 0x7F)
            }

            if (value > 0x10000000) {
                transport.write(value ushr 28 and 0x7F)
            }

            transport.write(END_SYSEX)
        }

        transport.flush()
    }

    override fun capability() {
        transport.write(START_SYSEX)
        transport.write(CAPABILITY_QUERY)
        transport.write(END_SYSEX)
        transport.flush()
    }

    override fun digitalWrite(portId: Int, value: Int) {
        transport.write(DIGITAL_MESSAGE or (portId and 0x0F))
        transport.sendValueAsTwo7bitBytes(value)
        transport.flush()
    }

    override fun requestFirmware() {
        transport.write(START_SYSEX)
        transport.write(REPORT_FIRMWARE)
        transport.write(END_SYSEX)
        transport.flush()
    }

    override fun pinState(pin: Int) {
        transport.write(START_SYSEX)
        transport.write(PIN_STATE_QUERY)
        transport.write(pin)
        transport.write(END_SYSEX)
        transport.flush()
    }

    override fun analogPinReport(pin: Int, enabled: Boolean) {
        transport.write(REPORT_ANALOG or pinToAnalogIndex(pin))
        transport.write(if (enabled) 1 else 0)
        transport.flush()
    }

    override fun analogReport(enabled: Boolean) {
        repeat(16) {
            transport.write(REPORT_ANALOG or it)
            transport.write(if (enabled) 1 else 0)
        }

        transport.flush()
    }

    override fun digitalPinReport(pin: Int, enabled: Boolean) {
        transport.write(REPORT_DIGITAL or pinToDigitalIndex(pin))
        transport.write(if (enabled) 1 else 0)
        transport.flush()
    }

    override fun digitalReport(enabled: Boolean) {
        repeat(16) {
            transport.write(REPORT_DIGITAL or it)
            transport.write(if (enabled) 1 else 0)
        }

        transport.flush()
    }

    override fun samplingInterval(interval: Int) {
        val value = max(MIN_SAMPLING_INTERVAL, min(interval, MAX_SAMPLING_INTERVAL))

        transport.write(START_SYSEX)
        transport.write(SAMPLING_INTERVAL)
        transport.sendValueAsTwo7bitBytes(value)
        transport.write(END_SYSEX)
        transport.flush()
    }

    override fun servoConfig(pin: Int, minPulse: Int, maxPulse: Int) {
        transport.write(START_SYSEX)
        transport.write(SERVO_CONFIG)
        transport.write(pin)
        transport.sendValueAsTwo7bitBytes(minPulse)
        transport.sendValueAsTwo7bitBytes(maxPulse)
        transport.write(END_SYSEX)
        transport.flush()
    }

    override fun mode(pin: Int, mode: PinMode) {
        transport.write(SET_PIN_MODE)
        transport.write(pin)
        transport.write(mode.ordinal)
        transport.flush()
    }

    override fun text(message: String) {
        transport.write(START_SYSEX)
        transport.write(STRING_DATA)

        val bytes = message.toByteArray(Charsets.US_ASCII)

        if (bytes.size > 15) {
            LOG.warn("Firmata 2.3.6 implementation has input buffer only 32 bytes so you can safely send only 15 characters log messages")
        }

        for (i in bytes.indices) {
            transport.sendValueAsTwo7bitBytes(bytes[i].toInt())
        }

        transport.write(END_SYSEX)
        transport.flush()
    }

    override fun twoWireConfig(delayInMicroseconds: Int) {
        require(delayInMicroseconds >= 0) { "delay cannot be less than 0 microseconds." }
        require(delayInMicroseconds <= 255) { "delay cannot be greater than 255 microseconds." }

        val delayLsb = delayInMicroseconds and 0x7F
        val delayMsb = if (delayInMicroseconds > 128) 1 else 0
        transport.write(START_SYSEX)
        transport.write(TWO_WIRE_CONFIG)
        transport.write(delayLsb)
        transport.write(delayMsb)
        transport.write(END_SYSEX)
        transport.flush()
    }

    override fun twoWireRead(slaveAddress: Int, register: Int, bytesToRead: Int, continuous: Boolean) {
        // https://github.com/firmata/protocol/blob/master/i2c.md

        transport.write(START_SYSEX)
        transport.write(TWO_WIRE_REQUEST)
        transport.write(slaveAddress)
        transport.write(if (continuous) TWO_WIRE_READ_CONTINUOUS else TWO_WIRE_READ)

        // TODO: replace hardcoded slave address (MSB) with generated one to support 10-bit mode

        if (register != REGISTER_NOT_SET) {
            transport.sendValueAsTwo7bitBytes(register)
        }

        transport.sendValueAsTwo7bitBytes(bytesToRead)

        transport.write(END_SYSEX)
        transport.flush()
    }

    override fun twoWireStopContinuous(slaveAddress: Int) {
        transport.write(START_SYSEX)
        transport.write(TWO_WIRE_REQUEST)
        transport.write(slaveAddress)
        transport.write(TWO_WIRE_STOP_READ_CONTINUOUS)
        transport.write(END_SYSEX)
        transport.flush()
    }

    override fun twoWireWrite(slaveAddress: Int, bytesToWrite: ByteArray) {
        transport.write(START_SYSEX)
        transport.write(TWO_WIRE_REQUEST)
        transport.write(slaveAddress)
        transport.write(TWO_WIRE_WRITE)
        transport.flush()

        // TODO: replace TWO_WIRE_WRITE with generated slave address (MSB) to support 10-bit mode.

        for (x in bytesToWrite.indices) {
            transport.sendValueAsTwo7bitBytes(bytesToWrite[x].toInt())
        }

        transport.write(END_SYSEX)
        transport.flush()
    }

    private val versionMessageHandler = Consumer<VersionMessageEvent> {
        LOG.info("firmware version. major={}, minor={}", it.major, it.minor)
    }

    private val firmwareMessageHandler = Consumer<FirmwareMessageEvent> {
        LOG.info("firmware message. major={}, minor={}, message={}", it.major, it.minor, it.message)
        capability()
    }

    private val pinCapabilityResponseHandler = Consumer<PinCapabilityResponseEvent> {
        val pin = FirmataPin(this@KuruboClient, it.pin)

        it.supportedModes.forEach(pin::addSupportedMode)

        synchronized(foundPins) {
            foundPins[pin.index] = pin

            if (pin.supportedModes.isEmpty()) {
                // if the pin has no supported modes, its initialization is already done.
                initializedPins.incrementAndGet()
            } else {
                // if the pin supports some modes, we ask for its current mode and value.
                pinStateRequestQueue.add(it.pin)
            }
        }
    }

    private val pinCapabilitiesFinishedHandler = Consumer<PinCapabilitiesFinishedEvent> {
        if (initializedPins.get() == foundPins.size) {
            analogMapping()
        } else {
            pinState(pinStateRequestQueue.poll())
        }
    }

    private val pinStateHandler = Consumer<PinStateEvent> {
        val pin = foundPins[it.pin] ?: return@Consumer

        pin.initMode(it.mode)

        if (pinStateRequestQueue.isNotEmpty()) {
            pinState(pinStateRequestQueue.poll())
        }

        if (initializedPins.incrementAndGet() == foundPins.size) {
            analogMapping()
        }
    }

    private val analogMappingHandler = Consumer<AnalogMappingEvent> {
        synchronized(analogMapping) {
            analogMapping.putAll(it.mapping)
        }

        ready.set(true)
        completable.complete(Unit)

        for (listener in listeners) {
            listener.onStart(this)
        }
    }

    private val analogMessageHandler = Consumer<AnalogMessageEvent> {
        synchronized(analogMapping) {
            if (it.pin in analogMapping) {
                val index = analogMapping[it.pin]!!

                val pin = foundPins[index] ?: return@Consumer

                if (pin.mode == PinMode.ANALOG) {
                    pin.updateValue(it.value)
                }
            }
        }
    }

    private val digitalMessageHandler = Consumer<DigitalMessageEvent> {
        val pin = foundPins[it.pin] ?: return@Consumer

        if (pin.mode == PinMode.INPUT || pin.mode == PinMode.PULL_UP) {
            pin.updateValue(it.value)
        }
    }

    private val stringMessageHandler = Consumer<StringMessageEvent> {
        synchronized(listeners) {
            for (listener in listeners) {
                listener.onMessageReceive(this, it.message)
            }
        }
    }

    private val twoWireMessageHandler = Consumer<TwoWireMessageEvent> {
        val device = twoWireDevices[it.address]
        device?.onReceive(it.register, it.message)
    }

    init {
        protocol.addHandler(versionMessageHandler)
        protocol.addHandler(firmwareMessageHandler)
        protocol.addHandler(pinCapabilityResponseHandler)
        protocol.addHandler(pinCapabilitiesFinishedHandler)
        protocol.addHandler(pinStateHandler)
        protocol.addHandler(analogMappingHandler)
        protocol.addHandler(analogMessageHandler)
        protocol.addHandler(digitalMessageHandler)
        protocol.addHandler(stringMessageHandler)
        addHandler(twoWireMessageHandler)
    }

    companion object {

        private const val TIMEOUT = 15000L

        private val LOG = LoggerFactory.getLogger(KuruboClient::class.java)
        private val DEFAULT_THREAD_FACTORY = DaemonThreadFactory("Firmata")
    }
}
