package br.tiagohm.kurubo.client.protocol.fsm.event

internal data class CustomMessageEvent(val message: ByteArray, val length: Int) : Event
