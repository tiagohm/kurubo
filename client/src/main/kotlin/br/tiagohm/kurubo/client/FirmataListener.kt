package br.tiagohm.kurubo.client

interface FirmataListener {

    fun onStart(firmata: Firmata) = Unit

    fun onStop(firmata: Firmata) = Unit

    fun onPinChange(firmata: Firmata, pin: Pin) = Unit

    fun onMessageReceive(firmata: Firmata, message: String) = Unit
}
