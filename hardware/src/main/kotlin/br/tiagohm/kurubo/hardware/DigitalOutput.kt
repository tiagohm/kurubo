package br.tiagohm.kurubo.hardware

import br.tiagohm.kurubo.client.Pin

interface DigitalOutput : Hardware {

    val pin: Pin

    fun on() {
        pin.value = 1
    }

    fun off() {
        pin.value = 0
    }

    fun toggle() {
        pin.value = pin.value xor 1 and 0x01
    }
}
