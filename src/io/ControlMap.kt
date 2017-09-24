package io

import io.OutputManager as out

private interface ControlBind

private data class KeyBind(val key: Int, val keyCodes: Set<Int>, val control: Control, val only: Boolean) : ControlBind

private data class MouseBind(val mouseButton: Int, val keyCodes: Set<Int>, val control: Control, val only: Boolean) : ControlBind

private data class MouseWheelBind(val wheelDirection: Int, val keyCodes: Set<Int>, val control: Control, val only: Boolean) : ControlBind

/*
Syntax:
<code>[:<more key codes separated by :>, end with -1 if this you don't want it to activate when any keys other than those specified are down]:<control>
 */
enum class ControlMap(path: String) {
    DEFAULT("/settings/controls/default.txt");

    private val binds = mutableListOf<ControlBind>()

    init {
        val text = ControlMap::class.java.getResource(path).readText()
        val lines = text.split(delimiters = "\n")
        var mode = 0
        for (a in lines) {
            val s = a.replace("\n", "").replace("\r", "")
            if (s.startsWith("//"))
                continue
            if (s.contains(char = ':')) {
                val split = s.split(delimiters = ':')
                val codes = split.subList(0, split.lastIndex).map { it.toInt() }
                val first = codes[0]
                val only = codes[codes.lastIndex] == -1
                // if statement because we don't want the -1 marker to be included in the actual key codes
                val end = codes.slice(1..if (only) codes.lastIndex - 1 else codes.lastIndex).toSet()
                val control = split[split.lastIndex]
                if (mode == 0)
                    binds.add(KeyBind(first, end, Control.valueOf(control), only))
                if (mode == 1)
                    binds.add(MouseBind(first, end, Control.valueOf(control), only))
                if (mode == 2)
                    // the if statement necessary so that -1 doesn't interfere. Messy but not really needing a fix
                    binds.add(MouseWheelBind(if (first == 2) -1 else first, end, Control.valueOf(control), only))
            } else {
                when (s) {
                    "k" -> mode = 0
                    "m" -> mode = 1
                    "mw" -> mode = 2
                }
            }
        }
    }

    fun translateKey(keyCode: Int, otherKeyCodes: MutableSet<Int>): Set<Control> {
        val controls = mutableSetOf<Control>()
        for (b in binds) {
            if (b is KeyBind) {
                if (b.key == keyCode) {
                    if (b.only) {
                        if (b.keyCodes == otherKeyCodes)
                            controls.add(b.control)
                    } else {
                        if (otherKeyCodes.containsAll(b.keyCodes))
                            controls.add(b.control)
                    }
                }
            }
        }
        return controls
    }

    fun translateMouse(mouseButton: Int, otherKeyCodes: Set<Int>): Set<Control> {
        val controls = mutableSetOf<Control>()
        for (b in binds) {
            if (b is MouseBind) {
                if (b.mouseButton == mouseButton) {
                    if (b.only) {
                        if (b.keyCodes == otherKeyCodes)
                            controls.add(b.control)
                    } else {
                        if (otherKeyCodes.containsAll(b.keyCodes))
                            controls.add(b.control)
                    }
                }
            }
        }
        return controls
    }

    fun translateMouseWheel(wheelDirection: Int, otherKeyCodes: Set<Int>): Set<Control> {
        val controls = mutableSetOf<Control>()
        for (b in binds) {
            if (b is MouseWheelBind) {
                if (b.wheelDirection == wheelDirection) {
                    if (b.only) {
                        if (otherKeyCodes == b.keyCodes)
                            controls.add(b.control)
                    } else {
                        if (otherKeyCodes.containsAll(b.keyCodes))
                            controls.add(b.control)
                    }
                }
            }
        }
        return controls
    }
}