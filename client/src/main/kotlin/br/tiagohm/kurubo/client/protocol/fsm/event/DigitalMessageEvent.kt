package br.tiagohm.kurubo.client.protocol.fsm.event

internal data class DigitalMessageEvent(override val pin: Int, val value: Int) : PinEvent
