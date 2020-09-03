package screen.attribute

import graphics.Image
import graphics.Renderer
import graphics.TextureRenderParams
import io.Control
import io.ControlEventType
import main.heightPixels
import main.widthPixels
import screen.ScreenManager
import screen.gui.*
import screen.mouse.Mouse

class AttributeDraggable(element: GuiElement) : Attribute(element) {

    var dragging = false
    var startingXPixel = 0
    var startingYPixel = 0
    var actOnStartingPlacement = Placement.Exact(0, 0)

    init {
        element.eventListeners.add(GuiInteractOnListener {
            if (it.event.control == Control.INTERACT) {
                if (this == ScreenManager.elementUnderMouse) {
                    if (it.event.type == ControlEventType.PRESS) {
                        dragging = true
                        startingXPixel = Mouse.xPixel
                        startingYPixel = Mouse.yPixel
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
                val nX = Mouse.xPixel - startingXPixel + actOnStartingPlacement.xPixel
                val nY = Mouse.yPixel - startingYPixel + actOnStartingPlacement.yPixel
                gui.parentElement.placement = Placement.Exact(nX, nY)
                gui.layout.recalculateExactPlacement(gui.parentElement)
            }
        })
        element.eventListeners.add(GuiCloseListener {
            dragging = false
        })
        element.eventListeners.add(GuiRenderListener { xPixel, yPixel, textureRenderParams ->
            if (dragging) {
                Renderer.renderTexture(Image.Gui.DRAG_GRIP_HIGHLIGHT, Mouse.xPixel - Image.Gui.DRAG_GRIP_HIGHLIGHT.widthPixels / 2, Mouse.yPixel - Image.Gui.DRAG_GRIP_HIGHLIGHT.heightPixels / 2, textureRenderParams
                        ?: TextureRenderParams.DEFAULT)
            }
        })
    }
}