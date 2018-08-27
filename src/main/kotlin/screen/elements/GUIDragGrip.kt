package screen.elements

import graphics.Image
import graphics.Renderer
import io.PressType
import main.Game
import screen.mouse.Mouse


class GUIDragGrip(parent: RootGUIElement,
                  name: String,
                  xAlignment: () -> Int, yAlignment: () -> Int,
                  open: Boolean = false,
                  layer: Int = parent.layer + 1,
                  val actOn: GUIWindow,
                  var keepInsideWindowBounds: Boolean = true) :
        GUIElement(parent, name, xAlignment, yAlignment, { WIDTH }, { HEIGHT }, open, layer) {

    var dragging = false
    var sXPixel = 0
    var sYPixel = 0
    var actOnSXPixel = 0
    var actOnSYPixel = 0

    override fun onInteractOn(type: PressType, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
        if (type == PressType.PRESSED) {
            dragging = true
            sXPixel = Mouse.xPixel
            sYPixel = Mouse.yPixel
            actOnSXPixel = actOn.xPixel
            actOnSYPixel = actOn.yPixel
        }
        else if (type == PressType.RELEASED) dragging = false
    }

    override fun onClose() {
        dragging = false
    }

    override fun render() {
        Renderer.renderTexture(Image.GUI.DRAG_GRIP, xPixel, yPixel)
    }

    override fun update() {
        if (dragging) {
            var nX = Mouse.xPixel - sXPixel + actOnSXPixel
            var nY = Mouse.yPixel - sYPixel + actOnSYPixel
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

    companion object {
        const val WIDTH = 4
        const val HEIGHT = 4
    }
}