package br.tiagohm.kurubo.client

data class TwoWireEvent(val device: TwoWire, val register: Int, val data: IntArray)
