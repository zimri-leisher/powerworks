package screen.attribute

import graphics.Image
import graphics.Renderer
import graphics.TextureRenderParams
import io.Control
import io.ControlEventType
import main.height
import main.width
import screen.ScreenManager
import screen.gui.*
import screen.mouse.Mouse

class AttributeResizable(element: GuiElement) : Attribute(element) {
    var dragging = false
    var startingX = 0
    var startingY = 0
    var actOnStartingDimensions = Dimensions.Exact(0, 0)

    init {
        element.eventListeners.add(GuiInteractOnListener {
            if (it.event.control == Control.INTERACT) {
                if (this == ScreenManager.elementUnderMouse) {
                    if (it.event.type == ControlEventType.PRESS) {
                        dragging = true
                        startingX = Mouse.x
                        startingY = Mouse.y
                        actOnStartingDimensions = gui.layout.getExactDimensions(gui.parentElement)
                    }
                }
                if (it.event.type == ControlEventType.RELEASE) {
                    dragging = false
                }
            }
        })
        element.eventListeners.add(GuiUpdateListener {
            if (dragging) {
                val dX = Mouse.x - startingX
                val dY = Mouse.y - startingY
                gui.parentElement.dimensions = Dimensions.Exact(actOnStartingDimensions.width + dX, actOnStartingDimensions.height + dY)
                gui.layout.recalculateExactDimensions(gui.parentElement)
            }
        })
        element.eventListeners.add(GuiCloseListener {
            dragging = false
        })
        element.eventListeners.add(GuiRenderListener { _, _, textureRenderParams ->
            if (dragging) {
                Renderer.renderTexture(Image.Gui.DIMENSION_DRAG_GRIP, Mouse.x - Image.Gui.DIMENSION_DRAG_GRIP.width / 2, Mouse.y - Image.Gui.DIMENSION_DRAG_GRIP.height / 2, textureRenderParams
                        ?: TextureRenderParams.DEFAULT)
            }
        })
    }
}