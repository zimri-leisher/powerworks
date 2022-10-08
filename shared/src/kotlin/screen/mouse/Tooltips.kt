package screen.mouse

import level.LevelManager
import level.PhysicalLevelObject
import main.GameState
import screen.ScreenLayer
import screen.ScreenManager
import screen.gui.*

object Tooltips {

    private val levelTooltipTemplates = sortedMapOf<Int, MutableList<(PhysicalLevelObject) -> String?>>()

    private val screenTooltipTemplates = sortedMapOf<Int, MutableList<(GuiElement) -> String?>>()

    private val toolTipDisplay = object : Gui(ScreenLayer.OVERLAY) {

        lateinit var background: ElementDefaultRectangle
        lateinit var text: ElementText

        init {
            define {
                keepInsideScreen()
                background = background {
                    dimensions = Dimensions.FitChildren.pad(2, 2)
                    text = text("", Placement.Align.Center)
                }
            }
        }
    }

    /**
     * @param f called for each LevelObject under the mouse. Returned text will be rendered at mouse
     */
    fun addLevelTooltipTemplate(f: (PhysicalLevelObject) -> String?, priority: Int = 0) {
        if (levelTooltipTemplates.get(priority) == null)
            levelTooltipTemplates.put(priority, mutableListOf())
        levelTooltipTemplates.get(priority)!!.add(f)
    }

    /**
     * @param f called for each GUIElement under the mouse. Returned text will be rendered at mouse
     */
    fun addScreenTooltipTemplate(f: (GuiElement) -> String?, priority: Int = 0) {
        if (screenTooltipTemplates.get(priority) == null)
            screenTooltipTemplates.put(priority, mutableListOf())
        screenTooltipTemplates.get(priority)!!.add(f)
    }

    fun update() {
        var text: String? = null
        val elements = ScreenManager.elementsUnderMouse
        outer@for(element in elements) {
            for (tooltips in screenTooltipTemplates.values) {
                for (tooltip in tooltips) {
                    text = tooltip(element)
                    if (text != null)
                        break@outer
                }
            }
        }
        if (text == null && GameState.currentState == GameState.INGAME) {
            val level = LevelManager.levelObjectUnderMouse
            if (level != null) {
                for (tooltips in levelTooltipTemplates.values) {
                    for (tooltip in tooltips) {
                        text = tooltip(level)
                        if (text != null)
                            break
                    }
                }
            }
        }
        if(text == null) {
            toolTipDisplay.open = false
        } else {
            toolTipDisplay.open = true
            toolTipDisplay.text.text = text
            toolTipDisplay.parentElement.placement = Placement.Exact(Mouse.x, Mouse.y)
            toolTipDisplay.layout.set()
        }
    }
}