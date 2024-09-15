package br.tiagohm.kurubo.client.protocol.fsm.event

internal data class ErrorEvent(val command: Int) : RuntimeException("Unknown control token has been received. Skipping. 0x%2x".format(command)), Event
