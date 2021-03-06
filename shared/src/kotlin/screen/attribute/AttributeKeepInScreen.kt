package screen.attribute

import main.Game
import misc.Geometry
import screen.gui.GuiChangeDimensionListener
import screen.gui.GuiChangePlacementListener
import screen.gui.GuiElement
import screen.gui.Placement
import java.lang.Integer.max
import java.lang.Integer.min

class AttributeKeepInScreen(element: GuiElement) : Attribute(element) {

    var hasMoved = false

    init {
//        element.gui.parentElement.eventListeners.add(GuiChangePlacementListener {
//            if (!hasMoved && !Geometry.contains(0, 0, Game.WIDTH, Game.HEIGHT, absoluteX, absoluteY, width, height)) {
//                placement = getNewPlacement()
//                gui.layout.recalculateExactPlacement(this)
//                hasMoved = true
//            } else {
//                hasMoved = false
//            }
//        })
//        element.gui.parentElement.eventListeners.add(GuiChangeDimensionListener {
//            if (!hasMoved && !Geometry.contains(0, 0, Game.WIDTH, Game.HEIGHT, absoluteX, absoluteY, width, height)) {
//                placement = getNewPlacement()
//                gui.layout.recalculateExactPlacement(this)
//                hasMoved = true
//            }
//        })
    }

    private fun getNewPlacement(): Placement.Exact {
        val placement = element.gui.placement
        val dimensions = element.gui.dimensions
        return Placement.Exact(max(0, min(Game.WIDTH - dimensions.width, placement.x)), max(0, min(Game.HEIGHT - dimensions.height, placement.y)))
    }
}