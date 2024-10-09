package br.tiagohm.kurubo.app

import com.github.rvesse.airline.SingleCommand

fun main(args: Array<String>) {
    val parser = SingleCommand.singleCommand(KuruboCommand::class.java)
    val kurubo = parser.parse(*args)
    kurubo.run()
    Thread.currentThread().join()
}
