package screen

import screen.elements.GUIDefaultTextureRectangle
import screen.elements.GUIResourceTypeSelection
import screen.elements.GUIWindow

internal object TestGUI : GUIWindow("Testing GUI", 0, 0, 60, 70, windowGroup = ScreenManager.Groups.INVENTORY) {

    init {
        GUIDefaultTextureRectangle(this, "test").apply {
            GUIResourceTypeSelection(this, "tester", { 1 }, { 54 }, allowRowGrowth = true)
        }
        generateDragGrip(layer = 5)
    }
}