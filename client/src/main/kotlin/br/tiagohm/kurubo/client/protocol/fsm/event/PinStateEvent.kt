package br.tiagohm.kurubo.client.protocol.fsm.event

import br.tiagohm.kurubo.client.PinMode

internal data class PinStateEvent(override val pin: Int, val mode: PinMode, val value: Int) : PinEvent
