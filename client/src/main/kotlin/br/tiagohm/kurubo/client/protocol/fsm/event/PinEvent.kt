package br.tiagohm.kurubo.client.protocol.fsm.event

sealed interface PinEvent : Event {

    val pin: Int
}
