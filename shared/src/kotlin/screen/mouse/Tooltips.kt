package screen.mouse

import level.LevelManager
import level.LevelObject
import main.State
import screen.ScreenManager
import screen.elements.RootGUIElement

object Tooltips {

    private val levelTooltipTemplates = sortedMapOf<Int, MutableList<(LevelObject) -> String?>>()

    private val screenTooltipTemplates = sortedMapOf<Int, MutableList<(RootGUIElement) -> String?>>()

    /**
     * @param f called for each LevelObject under the mouse. Returned text will be rendered at mouse
     */
    fun addLevelTooltipTemplate(f: (LevelObject) -> String?, priority: Int = 0) {
        if (levelTooltipTemplates.get(priority) == null)
            levelTooltipTemplates.put(priority, mutableListOf())
        levelTooltipTemplates.get(priority)!!.add(f)
    }

    /**
     * @param f called for each GUIElement under the mouse. Returned text will be rendered at mouse
     */
    fun addScreenTooltipTemplate(f: (RootGUIElement) -> String?, priority: Int = 0) {
        if (screenTooltipTemplates.get(priority) == null)
            screenTooltipTemplates.put(priority, mutableListOf())
        screenTooltipTemplates.get(priority)!!.add(f)
    }

    fun update() {
        var s: String? = null
        val screen = ScreenManager.getHighestElement(Mouse.xPixel, Mouse.yPixel, ScreenManager.getHighestWindow(Mouse.xPixel, Mouse.yPixel, { !it.transparentToInteraction }), { !it.transparentToInteraction })
        if (screen != null) {
            for (v in screenTooltipTemplates.values) {
                for (f in v) {
                    s = f(screen)
                    if (s != null)
                        break
                }
            }
        }
        if (s == null && State.CURRENT_STATE == State.INGAME) {
            val level = LevelManager.levelObjectUnderMouse
            if (level != null) {
                for (v in levelTooltipTemplates.values) {
                    for (f in v) {
                        s = f(level)
                        if (s != null)
                            break
                    }
                }
            }
        }
        if (s != null) {
            Mouse.text.text = s
            Mouse.text.open = true
            Mouse.background.alignments.updateDimension()
            Mouse.background.open = true
        } else {
            Mouse.text.open = false
            Mouse.background.open = false
        }
        Mouse.window.alignments.updatePosition()
    }
}