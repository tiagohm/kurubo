package br.tiagohm.kurubo.client.protocol.fsm.event

internal data class AnalogMessageEvent(override val pin: Int, val value: Int) : PinEvent
