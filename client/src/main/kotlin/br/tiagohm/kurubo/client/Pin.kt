package br.tiagohm.kurubo.client

interface Pin {

    val firmata: Firmata

    val index: Int

    val supportedModes: Set<PinMode>

    var mode: PinMode

    var value: Int

    fun servoMode(minPulse: Int, maxPulse: Int)

    fun supports(mode: PinMode): Boolean

    fun addPinListener(listener: PinListener)

    fun removePinListener(listener: PinListener)

    fun removeAllPinListeners()
}
