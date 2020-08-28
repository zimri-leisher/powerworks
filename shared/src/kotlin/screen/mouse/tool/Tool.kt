package screen.mouse.tool

import io.*
import level.LevelManager
import main.Game
import main.GameState
import screen.gui2.ElementLevelView
import screen.gui2.ScreenManager
import java.util.*

abstract class Tool(val use: List<Control>, val activators: List<Control> = listOf()) {
    constructor(use: Control, activators: List<Control> = listOf()) : this(listOf(use), activators)

    var priority = 0
    var currentlyHeldActivators = mutableListOf<Control>()
    var currentlyActive = false
    var activationPredicate: Tool.() -> Boolean = { true }

    abstract fun onUse(event: ControlEvent, mouseLevelXPixel: Int, mouseLevelYPixel: Int): Boolean

    open fun update() {}

    open fun renderBelow() {}

    open fun renderAbove() {}

    companion object : ControlEventHandler {
        val ALL = mutableListOf<Tool>()

        val controlEvents = LinkedList<ControlEvent>()

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
                InputManager.register(Companion, tool.activators.toList() + tool.use)
                tool.priority = priority
                ALL.add(tool)
                ALL.sortByDescending { it.priority }
            }
        }

        fun update() {
            var blocked = false
            for (tool in ALL) {
                for (event in controlEvents) {
                    if (event.control in tool.activators) {
                        if (event.type == ControlEventType.PRESS) {
                            tool.currentlyHeldActivators.add(event.control)
                        } else if (event.type == ControlEventType.RELEASE) {
                            tool.currentlyHeldActivators.remove(event.control)
                        }
                    }
                    tool.currentlyActive = tool.activationPredicate(tool) && tool.activators.all { it in tool.currentlyHeldActivators }
                    if (tool.currentlyActive && event.control in tool.use && !blocked) {
                        if (tool.onUse(event, LevelManager.mouseLevelXPixel, LevelManager.mouseLevelYPixel)) {
                            blocked = true
                        }
                    }
                }
            }
            controlEvents.clear()
            ALL.forEach { it.update() }
        }

        fun renderBelow() {
            ALL.forEach { it.renderBelow() }
        }

        fun renderAbove() {
            ALL.forEach { it.renderAbove() }
        }

        override fun handleControlEvent(event: ControlEvent) {
            if(GameState.currentState == GameState.INGAME) {
                if(ScreenManager.elementUnderMouse is ElementLevelView) {
                    controlEvents.add(event)
                }
            }
        }
    }
}