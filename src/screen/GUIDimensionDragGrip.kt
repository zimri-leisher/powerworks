package screen

import graphics.Image
import graphics.Renderer
import io.Mouse
import io.PressType
import main.Game
import misc.GeometryHelper

class GUIDimensionDragGrip(parent: RootGUIElement,
                           name: String,
                           xPixel: Int, yPixel: Int,
                           open: Boolean = false,
                           layer: Int = parent.layer + 1,
                           /** Whether or not the dimensions should be able to go into the negatives
                            * Note: also disallows 0 */
                           var keepInsideWindowBounds: Boolean = true,
                           var maintainDimensionRatio: Boolean = true) :
        GUIElement(parent, name, xPixel, yPixel, WIDTH, HEIGHT, open, layer) {

    var dragging = false
    var mXPixel = 0
    var mYPixel = 0
    var nWidthPixels = 0
    var nHeightPixels = 0
    var oWidthPixels = 0
    var oHeightPixels = 0

    override fun onMouseActionOn(type: PressType, xPixel: Int, yPixel: Int, button: Int) {
        if (type == PressType.PRESSED) {
            dragging = true
            mXPixel = Mouse.xPixel
            mYPixel = Mouse.yPixel
            nWidthPixels = parent.widthPixels
            nHeightPixels = parent.heightPixels
            oWidthPixels = parent.widthPixels
            oHeightPixels = parent.heightPixels
        } else if (type == PressType.RELEASED) {
            dragging = false
            if (parent is GUIElement) {
                if (keepInsideWindowBounds &&
                        !GeometryHelper.contains(parentWindow.xPixel, parentWindow.yPixel, parentWindow.widthPixels, parentWindow.heightPixels,
                                parent.xPixel, parent.yPixel, nWidthPixels, nHeightPixels)) {
                    nWidthPixels = parentWindow.widthPixels - parent.xPixel
                    nHeightPixels = parentWindow.heightPixels - parent.yPixel
                }
                parent.widthPixels = nWidthPixels
                parent.heightPixels = nHeightPixels
            } else {
                if (keepInsideWindowBounds &&
                        !GeometryHelper.contains(0, 0, Game.WIDTH, Game.HEIGHT,
                                parent.xPixel, parent.yPixel, nWidthPixels, nHeightPixels)) {
                    nWidthPixels = Game.WIDTH - parentWindow.xPixel
                    nHeightPixels = Game.HEIGHT - parentWindow.yPixel
                }
                parentWindow.widthPixels = nWidthPixels
                parentWindow.heightPixels = nHeightPixels
            }
            relXPixel += (nWidthPixels - oWidthPixels)
            relYPixel += (nHeightPixels - oHeightPixels)
        }
    }

    override fun onMouseActionOff(type: PressType, xPixel: Int, yPixel: Int, button: Int) {
        if (type == PressType.RELEASED)
            dragging = false
    }

    override fun render() {
        if (!dragging)
            Renderer.renderTexture(Image.GUI.DIMENSION_DRAG_GRIP, xPixel, yPixel)
        else {
            Renderer.renderEmptyRectangle(parent.xPixel, parent.yPixel, nWidthPixels, nHeightPixels)
        }
    }

    override fun update() {
        if (dragging) {
            val dX = Mouse.xPixel - mXPixel
            val dY = Mouse.yPixel - mYPixel
            /*
            if(maintainDimensionRatio) {
                if(Math.abs(dX) > Math.abs(dY))
                    dY = dX
                else
                    dY = dX
            }
            */
            nWidthPixels += dX
            nHeightPixels += dY
            nWidthPixels = Math.max(0, nWidthPixels)
            nHeightPixels = Math.max(0, nHeightPixels)
            mXPixel = Mouse.xPixel
            mYPixel = Mouse.yPixel
        }
    }

    companion object {
        const val WIDTH = 4
        const val HEIGHT = 4
    }
}