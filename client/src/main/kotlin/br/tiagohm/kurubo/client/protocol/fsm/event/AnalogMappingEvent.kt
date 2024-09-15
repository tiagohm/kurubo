package br.tiagohm.kurubo.client.protocol.fsm.event

internal data class AnalogMappingEvent(val mapping: Map<Int, Int>) : Event
