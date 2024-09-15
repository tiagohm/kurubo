package br.tiagohm.kurubo.client.protocol.fsm.event

import br.tiagohm.kurubo.client.PinMode

internal data class PinCapabilityResponseEvent(override val pin: Int, val supportedModes: Set<PinMode>) : PinEvent
