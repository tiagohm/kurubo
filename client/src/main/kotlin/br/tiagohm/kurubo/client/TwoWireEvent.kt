package br.tiagohm.kurubo.client

data class TwoWireEvent(override val device: TwoWire, val register: Int, val data: IntArray) : TwoWireable
