package io

import java.io.PrintStream

object OutputManager {

    var out: PrintStream = System.out

    fun println(message: Any?) {
        out.println(message)
    }

    fun print(message: Any?) {
        out.print(message)
    }
}