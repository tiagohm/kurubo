package br.tiagohm.kurubo.client

data class InvalidPinWriteException(val pin: Pin, val mode: PinMode) : FirmataException("pin ${pin.index} is in $mode mode and its value cannot be set")
