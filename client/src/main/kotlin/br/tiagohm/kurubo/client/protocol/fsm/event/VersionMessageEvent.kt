package br.tiagohm.kurubo.client.protocol.fsm.event

internal data class VersionMessageEvent(val major: Int, val minor: Int) : Event
