package br.tiagohm.kurubo.client.protocol.fsm.event

internal data class FirmwareMessageEvent(val major: Int, val minor: Int, val message: String) : Event
