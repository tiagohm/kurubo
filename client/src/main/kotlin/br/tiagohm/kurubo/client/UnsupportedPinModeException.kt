package br.tiagohm.kurubo.client

data class UnsupportedPinModeException(override val pin: Pin, val mode: PinMode) : FirmataException("pin ${pin.index} does not support mode $mode"), Pinnable
