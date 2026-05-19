package com.michaelsgroi.test.junitility

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.michaelsgroi.test.junitility.commands.CsvCommand
import com.michaelsgroi.test.junitility.commands.JsonCommand
import com.michaelsgroi.test.junitility.commands.ReportCommand

class JunitilityCommand : CliktCommand(name = "junitility", help = "JUnit test report utilities") {
    val debug by option("--debug", help = "Enable debug output").flag(default = false)

    override fun run() {}
}

fun main(args: Array<String>) {
    JunitilityCommand()
        .subcommands(ReportCommand(), CsvCommand(), JsonCommand())
        .main(args)
}
