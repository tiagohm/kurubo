package br.tiagohm.kurubo.client

interface PinListener {

    fun onModeChange(pin: Pin) = Unit

    fun onValueChange(pin: Pin) = Unit

    fun interface ModeChange : PinListener {

        override fun onModeChange(pin: Pin)
    }

    fun interface ValueChange : PinListener {

        override fun onValueChange(pin: Pin)
    }
}
