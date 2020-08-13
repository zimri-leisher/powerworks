package screen.mouse.tool

import io.*
import level.LevelManager
import main.Game
import java.util.*

abstract class Tool(val use: List<Control>, val activators: List<Control> = listOf()) {
    constructor(use: Control, activators: List<Control> = listOf()) : this(listOf(use), activators)

    var priority = 0
    var currentlyHeldActivators = mutableListOf<Control>()
    var currentlyActive = false
    var activationPredicate: Tool.() -> Boolean = { true }

    abstract fun onUse(control: Control, type: PressType, mouseLevelXPixel: Int, mouseLevelYPixel: Int): Boolean

    open fun update() {}

    open fun renderBelow() {}

    open fun renderAbove() {}

    companion object : ControlHandler {
        val ALL = mutableListOf<Tool>()

        val presses = LinkedList<ControlPress>()

        init {
            addTool(BlockPicker)
            addTool(BlockPlacer, 10)
            addTool(BlockRemover, 9)
            addTool(EntityController, 11)
            addTool(Interactor, 0)
            addTool(ResourceNodeEditor, 12)
            addTool(Selector, 1)
            addTool(EntityPlacer, 8)
            addTool(Teleporter, 11)
        }

        fun addTool(tool: Tool, priority: Int = 0) {
            if (!Game.IS_SERVER) {
                InputManager.registerControlPressHandler(Companion, ControlPressHandlerType.LEVEL_ANY_UNDER_MOUSE, tool.activators.toList() + tool.use)
                tool.priority = priority
                ALL.add(tool)
                ALL.sortByDescending { it.priority }
            }
        }

        fun update() {
            var blocked = false
            for (tool in ALL) {
                for (press in presses) {
                    if (press.control in tool.activators) {
                        if (press.pressType == PressType.PRESSED) {
                            tool.currentlyHeldActivators.add(press.control)
                        } else if (press.pressType == PressType.RELEASED) {
                            tool.currentlyHeldActivators.remove(press.control)
                        }
                    }
                    tool.currentlyActive = tool.activationPredicate(tool) && tool.activators.all { it in tool.currentlyHeldActivators }
                    if (tool.currentlyActive && press.control in tool.use && !blocked) {
                        if (tool.onUse(press.control, press.pressType, LevelManager.mouseLevelXPixel, LevelManager.mouseLevelYPixel)) {
                            blocked = true
                        }
                    }
                }
            }

            presses.clear()
            ALL.forEach { it.update() }
        }

        fun renderBelow() {
            ALL.forEach { it.renderBelow() }
        }

        fun renderAbove() {
            ALL.forEach { it.renderAbove() }
        }

        override fun handleControl(p: ControlPress) {
            presses.add(p)
        }
    }
}