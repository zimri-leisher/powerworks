package screen.elements

import graphics.Image
import graphics.Renderer
import io.*
import main.Game
import main.heightPixels
import main.widthPixels
import screen.mouse.Mouse

class GUIDragGrip(parent: RootGUIElement,
                  name: String,
                  xAlignment: Alignment, yAlignment: Alignment,
                  open: Boolean = false,
                  layer: Int = parent.layer + 1,
                  val actOn: GUIWindow,
                  var keepInsideWindowBounds: Boolean = true) :
        GUIElement(parent, name, xAlignment, yAlignment, { WIDTH }, { HEIGHT }, open, layer),
ControlPressHandler{

    var dragging = false
    var startingXPixel = 0
    var startingYPixel = 0
    var actOnStartingXPixel = 0
    var actOnStartingYPixel = 0

    init {
        InputManager.registerControlPressHandler(this, ControlPressHandlerType.GLOBAL, Control.Group.INTERACTION)
    }

    override fun onInteractOn(type: PressType, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
        if (type == PressType.PRESSED) {
            dragging = true
            startingXPixel = Mouse.xPixel
            startingYPixel = Mouse.yPixel
            actOnStartingXPixel = actOn.xPixel
            actOnStartingYPixel = actOn.yPixel
        } else if (type == PressType.RELEASED) {
            dragging = false
        }
    }

    override fun onClose() {
        dragging = false
    }

    override fun render() {
        if (dragging || mouseOn) {
            Renderer.renderTexture(Image.GUI.DRAG_GRIP_HIGHLIGHT, xPixel - 1, yPixel - 1)
        } else {
            Renderer.renderTexture(Image.GUI.DRAG_GRIP, xPixel, yPixel)
        }
    }

    override fun update() {
        if (dragging) {
            var nX = Mouse.xPixel - startingXPixel + actOnStartingXPixel
            var nY = Mouse.yPixel - startingYPixel + actOnStartingYPixel
            if (keepInsideWindowBounds) {
                nX = Math.max(0,
                        Math.min(
                                Game.WIDTH - actOn.widthPixels,
                                nX
                        ))
                nY = Math.max(0,
                        Math.min(
                                Game.HEIGHT - actOn.heightPixels,
                                nY
                        ))
            }
            actOn.alignments.x = { nX }
            actOn.alignments.y = { nY }
        }
    }

    override fun handleControlPress(p: ControlPress) {
        if(p.pressType == PressType.RELEASED && dragging) {
            dragging = false
        }
    }
    companion object {
        val WIDTH = Image.GUI.DRAG_GRIP.widthPixels
        val HEIGHT = Image.GUI.DRAG_GRIP.heightPixels

    }
}