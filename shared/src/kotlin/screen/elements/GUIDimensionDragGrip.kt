package screen.elements

import graphics.Image
import graphics.Renderer
import io.ControlEvent
import io.ControlEventType
import main.Game
import screen.mouse.Mouse

class GUIDimensionDragGrip(parent: RootGUIElement,
                           name: String,
                           xAlignment: Alignment, yAlignment: Alignment,
                           open: Boolean = false,
                           layer: Int = parent.layer + 1,
                           val actOn: GUIWindow,
                           /** Whether or not the dimensions should be able to go into the negatives
                            * Note: also disallows 0 */
                           var keepInsideWindowBounds: Boolean = true,
                           var maintainDimensionRatio: Boolean = true) :
        GUIElement(parent, name, xAlignment, yAlignment, { WIDTH }, { HEIGHT }, open, layer) {

    var dragging = false
    var startXPixel = 0
    var startYPixel = 0
    var nWidthPixels = 0
    var nHeightPixels = 0

    override fun onInteractOn(event: ControlEvent, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
        if (event.type == ControlEventType.PRESS) {
            dragging = true
            startXPixel = Mouse.xPixel
            startYPixel = Mouse.yPixel
            nWidthPixels = actOn.widthPixels
            nHeightPixels = actOn.heightPixels
        } else if (event.type == ControlEventType.RELEASE) {
            dragging = false
            if (keepInsideWindowBounds) {
                nWidthPixels = Math.min(Game.WIDTH, nWidthPixels)
                nHeightPixels = Math.min(Game.HEIGHT, nHeightPixels)
            }
            actOn.alignments.width = { nWidthPixels }
            actOn.alignments.height = { nHeightPixels }
        }
    }

    /*
    override fun onInteractOff(xPixel: Int, yPixel: Int, type: PressType, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
        if (dragging && type == PressType.RELEASED) {
            dragging = false
            if (keepInsideWindowBounds) {
                nWidthPixels = Math.min(Game.WIDTH, nWidthPixels)
                nHeightPixels = Math.min(Game.HEIGHT, nHeightPixels)
            }
            actOn.alignments.width = { nWidthPixels }
            actOn.alignments.height = { nHeightPixels }
        }
    }
     */

    override fun render() {
        if (!dragging)
            Renderer.renderTexture(Image.GUI.DIMENSION_DRAG_GRIP, xPixel, yPixel, localRenderParams)
        else {
            Renderer.renderEmptyRectangle(actOn.xPixel, actOn.yPixel, nWidthPixels, nHeightPixels, params = localRenderParams)
        }
    }

    override fun update() {
        if (dragging) {
            nWidthPixels = Mouse.xPixel - startXPixel + actOn.widthPixels
            nHeightPixels = Mouse.yPixel - startYPixel + actOn.heightPixels
            nWidthPixels = Math.max(0, nWidthPixels)
            nHeightPixels = Math.max(0, nHeightPixels)
        }
    }

    companion object {
        const val WIDTH = 4
        const val HEIGHT = 4
    }
}