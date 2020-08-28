package io

import data.FileManager
import data.GameDirectoryIdentifier
import serialization.ReadException
import java.io.FileReader
import java.io.FileWriter
import java.nio.file.Files

enum class Modifier(val button: String) {
    SHIFT("SHIFT"), ALT("ALT"), CTRL("CTRL")
}

data class ControlBind(val result: Control,
                       val button: String,
                       val modifiers: Set<String>,
                       val optionalModifiers: Set<String>,
                       val toggle: Boolean,
                       val double: Boolean)

enum class ControlEventType {
    PRESS, RELEASE, HOLD
}

data class ControlEvent(val control: Control, val type: ControlEventType)

class ControlMap(val name: String) {

    var activeControls = mutableListOf<Control>()
    val binds = mutableListOf<ControlBind>()

    init {
        load()
    }

    fun load() {
        val fileInput = FileReader(FileManager.tryGetFile(GameDirectoryIdentifier.CONTROLS, "$name.txt")
                ?: throw ReadException("Unable to load control file $name.txt"))
        for (line in fileInput.readLines()) {
            if (line.isBlank() || line.startsWith("//")) {
                continue
            }
            // each line specifies a control
            val words = line.split(" ").map { it.replace(" ", "") }
            val control = Control.valueOf(words[0].toUpperCase())
            val button = words[1].replace("2x", "")
            val double = words[1].contains("2x")
            val modifiers = if (words.lastIndex == 1) listOf() else words[2].split(",").map { it.replace(",", "") }
            val optionalModifiers = modifiers.filter { it.startsWith("?") }.map { it.replace("?", "") }
            val requiredModifiers = modifiers.filterNot { it.startsWith("?") }
            val toggle = words.any { it == "TOGGLE" }
            binds.add(ControlBind(control, button, requiredModifiers.toSet(), optionalModifiers.toSet(), toggle, double))
        }
    }

    fun save() {
        val text = StringBuilder()
        for (bind in binds) {
            text.append(bind.result.name + " ")
            text.append((if (bind.double) "2x" else "") + bind.button + " ")
            text.append(bind.modifiers.joinToString(","))
            text.append(bind.optionalModifiers.joinToString("") { ",?$it" })
            if (bind.toggle) {
                text.append(" TOGGLE")
            }
            text.appendln()
        }
        var file = FileManager.tryGetFile(GameDirectoryIdentifier.CONTROLS, "$name.txt")
        if (file == null) {
            file = Files.createFile(FileManager.fileSystem.getPath(GameDirectoryIdentifier.CONTROLS).resolve("$name.txt")).toFile()!!
        }
        val output = FileWriter(file)
        output.write(text.toString())
        output.flush()
    }

    fun getControlEvents(state: InputState): List<ControlEvent> {
        val newControls = mutableListOf<Control>()
        for (bind in binds) {
            val events = state.getEventsFor(bind.button)
            // if the trigger button is down. we check if the press event is in the events because
            // of the scroll wheel. calling state.isDown for a scroll button never is true, but they still have the
            // press event
            if (InputEventType.PRESS !in events && !state.isDown(bind.button)) {
                continue
            }
            // first, make sure that CTRL/ALT/SHIFT all match exactly
            // then check everything else matches
            if (Modifier.values().filter { it.button !in bind.optionalModifiers }.any { state.isDown(it) != it.button in bind.modifiers }
                    || bind.modifiers.any { !state.isDown(it) }) {
                continue
            }
            // if the bind is activated by a double click
            if (bind.double) {
                if (InputEventType.PRESS in events) {
                    if (InputEventType.DOUBLE_TAP !in events) {
                        // if it was a press but not a double tap
                        continue
                    }
                } else if (InputEventType.RELEASE in events) {
                    // if it was just released
                    continue
                }
            }

            if (bind.toggle) {
                if (InputEventType.PRESS in events) {
                    if (bind.result in activeControls) {
                        // if it was just pressed and was previously active
                        continue
                    }
                }
            }
            newControls.add(bind.result)
        }
        val events = mutableListOf<ControlEvent>()
        for (newControl in newControls) {
            if (newControl !in activeControls) {
                events.add(ControlEvent(newControl, ControlEventType.PRESS))
            } else {
                events.add(ControlEvent(newControl, ControlEventType.HOLD))
            }
        }
        for (oldControl in activeControls) {
            if (oldControl !in newControls) {
                events.add(ControlEvent(oldControl, ControlEventType.RELEASE))
            }
        }
        activeControls = newControls
        return events
    }

    companion object {
        val DEFAULT = ControlMap("default")
        val TEXT_EDITOR = ControlMap("texteditor")
    }
}