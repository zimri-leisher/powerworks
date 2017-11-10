package screen

import io.Control
import io.ControlPress
import io.ControlPressHandler
import io.PressType
import level.LevelObject
import level.moving.MovingObject

class ViewWindow(name: String,
                 xPixel: Int, yPixel: Int,
                 widthPixels: Int, heightPixels: Int,
                 var camera: LevelObject,
                 zoomLevel: Int = 10,
                 open: Boolean = false,
                 layer: Int = 0,
                 windowGroup: WindowGroup) :
        GUIWindow(name, xPixel, yPixel, widthPixels, heightPixels, open, layer, windowGroup),
        ControlPressHandler {

    val view = GUIView(this.rootChild, name,
            0, 0,
            widthPixels, heightPixels,
            camera, zoomLevel, open)
    val outline = GUIOutline(view, name + " outline", open = open)
    val dragGrip = generateDragGrip(rootChild, 2)
    val closeButton = generateCloseButton(rootChild, 2)
    val dimensionDragGrip = generateDimensionDragGrip(rootChild, 2)
    val nameText = GUIText(view, name, 1, 4, name, layer = 1)

    init {
        ScreenManager.registerControlPressHandler(this, Control.UP, Control.DOWN, Control.LEFT, Control.RIGHT)
        nameText.transparentToInteraction = true
        view.adjustDimensions = true
    }

    override fun onDimensionChange(oldWidth: Int, oldHeight: Int) {
        dragGrip.relXPixel = widthPixels - GUIDragGrip.WIDTH - 1
        closeButton.relXPixel = widthPixels - GUIDragGrip.WIDTH - GUICloseButton.WIDTH - 2
        dimensionDragGrip.relXPixel = widthPixels - GUIDragGrip.WIDTH - GUICloseButton.WIDTH - GUIDimensionDragGrip.WIDTH - 3
    }

    override fun handleControlPress(p: ControlPress) {
        if (p.pressType == PressType.RELEASED || camera !is MovingObject)
            return
        val c = p.control
        val m = camera as MovingObject
        if (c == Control.UP) {
            m.yVel--
        } else if (c == Control.DOWN) {
            m.yVel++
        } else if (c == Control.RIGHT) {
            m.xVel++
        } else if (c == Control.LEFT) {
            m.xVel--
        }
    }
}