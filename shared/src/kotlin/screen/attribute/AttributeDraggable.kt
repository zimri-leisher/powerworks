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

class AttributeDraggable(element: GuiElement) : Attribute(element) {

    var dragging = false
    var startingX = 0
    var startingY = 0
    var actOnStartingPlacement = Placement.Exact(0, 0)

    init {
        element.eventListeners.add(GuiInteractOnListener {
            if (it.event.control == Control.INTERACT) {
                if (this == ScreenManager.elementUnderMouse) {
                    if (it.event.type == ControlEventType.PRESS) {
                        dragging = true
                        startingX = Mouse.x
                        startingY = Mouse.y
                        actOnStartingPlacement = gui.layout.getExactPlacement(gui.parentElement)
                    }
                }
                if (it.event.type == ControlEventType.RELEASE) {
                    dragging = false
                }
            }
        })
        element.eventListeners.add(GuiUpdateListener {
            if (dragging) {
                val nX = Mouse.x - startingX + actOnStartingPlacement.x
                val nY = Mouse.y - startingY + actOnStartingPlacement.y
                gui.parentElement.placement = Placement.Exact(nX, nY)
                gui.layout.recalculateExactPlacement(gui.parentElement)
            }
        })
        element.eventListeners.add(GuiCloseListener {
            dragging = false
        })
        element.eventListeners.add(GuiRenderListener { x, y, textureRenderParams ->
            if (dragging) {
                Renderer.renderTexture(Image.Gui.DRAG_GRIP_HIGHLIGHT, Mouse.x - Image.Gui.DRAG_GRIP_HIGHLIGHT.width / 2, Mouse.y - Image.Gui.DRAG_GRIP_HIGHLIGHT.height / 2, textureRenderParams
                        ?: TextureRenderParams.DEFAULT)
            }
        })
    }
}