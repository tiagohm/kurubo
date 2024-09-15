package br.tiagohm.kurubo.hardware

import br.tiagohm.kurubo.client.Firmata

interface Hardware {

    val name: String

    val firmata: Firmata

    fun start()

    fun stop()
}
