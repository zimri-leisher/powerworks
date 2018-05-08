package io

import data.FileManager
import data.GameDirectoryIdentifier
import io.OutputManager as out

private data class ControlBind(val code: String, val otherCodes: Set<String>, val notOtherCodes: Set<String>, val control: Control, val only: Boolean)

enum class ControlMap(path: String) {
    DEFAULT("default");

    private val binds = mutableListOf<ControlBind>()

    init {
        val lines = FileManager.fileSystem.getPath(GameDirectoryIdentifier.CONTROLS).resolve("$path.txt").toFile().readLines()
        var mode = 0
        for (a in lines) {
            val s = a.replace("\n", "").replace("\r", "")
            if (s.startsWith("//"))
                continue
            if (s.contains(char = ':')) {
                val split = s.split(':').map { it.replace(":", "") }
                val only = split[split.lastIndex - 1] == "ONLY"
                val code = split[0]
                val otherCodes = split.dropLast(if (only) 2 else 1).drop(1).filterNot { it.startsWith('!') }.toSet()
                val notOtherCodes = split.filter { it.startsWith('!') }.map { it.replace("!", "") }.toSet()
                val control = Control.valueOf(split.last())
                val c = ControlBind(code, otherCodes, notOtherCodes, control, only)
                binds.add(c)
            }
        }
    }

    /**
     * Returns the controls that match the current codes
     */
    fun translate(code: String, otherCodes: MutableSet<String>): Set<Control> {
        val controls = mutableSetOf<Control>()
        val upper = otherCodes.map { it.toUpperCase() }
        for (b in binds) {
            if(b.code == code && upper.containsAll(b.otherCodes) && b.notOtherCodes.all { !upper.contains(it) }) {
                if(b.only) {
                    if(b.otherCodes.size == upper.size) {
                        controls.add(b.control)
                    }
                } else {
                    controls.add(b.control)
                }
            }
        }
        return controls
    }
}