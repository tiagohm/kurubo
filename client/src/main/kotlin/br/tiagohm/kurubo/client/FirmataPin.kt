package br.tiagohm.kurubo.client

import java.util.*
import java.util.concurrent.ConcurrentHashMap

internal data class FirmataPin(override val firmata: KuruboClient, override val index: Int) : Pin {

    private val listeners = ConcurrentHashMap.newKeySet<PinListener>(1)
    private val pinModeLock = Any()
    private val pinValueLock = Any()

    @Volatile private var currentMode = PinMode.UNSUPPORTED
    @Volatile private var currentValue = 0

    override val supportedModes: MutableSet<PinMode> = Collections.synchronizedSet(EnumSet.noneOf(PinMode::class.java))

    override var mode
        get() = currentMode
        set(mode) {
            // Arduino defaults (https://www.arduino.cc/en/Reference/ServoAttach)
            updateMode(mode, 544, 2400)
        }

    override var value
        get() = currentValue
        set(value) {
            synchronized(pinValueLock) {
                when (currentMode) {
                    PinMode.OUTPUT -> {
                        val isOn = value > 0

                        if (updateValue(if (isOn) 1 else 0)) {
                            val portId = index / 8
                            val pinInPort = index % 8
                            var portValue = 0

                            repeat(8) {
                                val p = firmata.pinAt(portId * 8 + it)

                                if (p.mode == PinMode.OUTPUT && p.value > 0) {
                                    portValue = portValue or (1 shl it)
                                }
                            }

                            val bit = 1 shl pinInPort

                            portValue = if (isOn) {
                                portValue or bit
                            } else {
                                portValue and bit.inv()
                            }

                            firmata.digitalWrite(portId, portValue)
                        }
                    }
                    PinMode.ANALOG,
                    PinMode.PWM,
                    PinMode.SERVO -> {
                        if (updateValue(value)) {
                            firmata.analogWrite(index, value)
                        }
                    }
                    else -> {
                        throw InvalidPinWriteException(this, currentMode)
                    }
                }
            }
        }

    override fun servoMode(minPulse: Int, maxPulse: Int) {
        updateMode(PinMode.SERVO, minPulse, maxPulse)
    }

    private fun updateMode(mode: PinMode, minPulse: Int, maxPulse: Int) {
        if (supports(mode)) {
            synchronized(pinModeLock) {
                if (currentMode != mode) {
                    if (mode == PinMode.SERVO) {
                        firmata.servoConfig(index, minPulse, maxPulse)
                        // The currentValue for a servo is unknown as the motor is
                        // send to the 1.5ms position when pinStateRequest is invoked
                        currentValue = -1
                    }

                    firmata.mode(index, mode)
                    currentMode = mode

                    firmata.pinChanged(this)

                    for (listener in listeners) {
                        listener.onModeChange(this)
                    }

                    firmata.pinState(index)
                }
            }
        } else {
            throw UnsupportedPinModeException(this, mode)
        }
    }

    override fun supports(mode: PinMode): Boolean {
        return mode in supportedModes
    }

    override fun addPinListener(listener: PinListener) {
        listeners.add(listener)
    }

    override fun removePinListener(listener: PinListener) {
        listeners.remove(listener)
    }


    override fun removeAllPinListeners() {
        listeners.clear()
    }

    fun addSupportedMode(mode: PinMode) {
        supportedModes.add(mode)
    }

    fun initMode(mode: PinMode) {
        currentMode = mode
    }

    fun initValue(value: Int) {
        currentValue = value
    }

    fun updateValue(value: Int): Boolean {
        return if (value != currentValue) {
            currentValue = value

            firmata.pinChanged(this)

            for (listener in listeners) {
                listener.onValueChange(this)
            }

            true
        } else {
            false
        }
    }

    override fun toString() = "FirmataPin(index=$index, value=$currentValue, mode=$currentMode)"
}
