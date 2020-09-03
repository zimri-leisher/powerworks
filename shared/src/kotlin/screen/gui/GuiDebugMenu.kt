package screen.gui

import graphics.Renderer
import graphics.text.TextManager
import io.*
import main.DebugCode
import main.Game
import screen.ScreenLayer
import kotlin.math.ceil

object GuiDebugMenu : Gui(ScreenLayer.OVERLAY), ControlEventHandler {
    init {
        InputManager.register(this, Control.TURN_OFF_DEBUG_INFO)
        define {
            list(Placement.Origin, HorizontalAlign.LEFT) {
                button("none", { Game.currentDebugCode = DebugCode.NONE })
                button("gui info", { Game.currentDebugCode = DebugCode.SCREEN_INFO })
                button("show controls", { Game.currentDebugCode = DebugCode.CONTROLS_INFO })
                button("chunk info", { Game.currentDebugCode = DebugCode.CHUNK_INFO })
                button("level info", { Game.currentDebugCode = DebugCode.LEVEL_INFO })
            }
        }
    }

    override fun handleControlEvent(event: ControlEvent) {
        if (event.type == ControlEventType.PRESS && event.control == Control.TURN_OFF_DEBUG_INFO) {
            open = !open
        }
    }

}

object GuiDebugInfo : Gui(ScreenLayer.OVERLAY) {
    private val debugInfo = mutableMapOf<Any, List<String>>()

    init {
        define {
            placement = Placement.Align(HorizontalAlign.LEFT, VerticalAlign.TOP)
            onRender { xPixel, yPixel, _ ->
                var y = 0
                for ((_, text) in debugInfo) {
                    y += ceil((TextManager.getFont().charHeight + 1) * text.size).toInt()
                    Renderer.renderText(text.joinToString(separator = "\n"), xPixel, yPixel - y)
                }
                debugInfo.clear()
            }
        }
    }

    fun show(from: Any, information: List<String>) = debugInfo.put(from, information)

}