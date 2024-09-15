package br.tiagohm.kurubo.hardware

interface Pollable : Runnable {

    val pollInterval: Long
}
