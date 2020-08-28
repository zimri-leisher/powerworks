package screen

import graphics.Renderer
import graphics.TextureRenderParams
import level.Level
import level.block.FarseekerBlock
import screen.elements.AutoFormatGUIWindow
import screen.elements.GUIDefaultTextureRectangle
import screen.elements.GUIElement
import screen.elements.RootGUIElement

class FarseekerBlockGUI(var block: FarseekerBlock) : AutoFormatGUIWindow("FarseekerBlock GUI window", { 0 }, { 0 }) {

    init {

    }

    class PlanetSelectorGUI(parent: RootGUIElement, open: Boolean = false, layer: Int = parent.layer + 1) : GUIElement(parent, "Planet selector gui", 0, 0, 80, 80, open, layer) {

        val availableLevels = listOf<Level>()

        init {
            GUIDefaultTextureRectangle(this, "planet selector gui background").apply {

            }
        }
    }
}