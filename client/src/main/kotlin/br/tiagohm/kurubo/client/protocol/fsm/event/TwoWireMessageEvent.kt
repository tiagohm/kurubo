package br.tiagohm.kurubo.client.protocol.fsm.event

internal data class TwoWireMessageEvent(val address: Int, val register: Int, val message: IntArray) : Event
