package screen

import io.*
import level.LevelObject
import level.moving.MovingObject
import screen.elements.*

/**
 * A pre-built GUIWindow for interaction with the level.
 * Has a GUILevelView, a GUIOutline around it, some miscellaneous controls for movement, resizing, etc.
 * By default, the IngameGUI has 4 of these. Control.TOGGLE_VIEW_CONTROLS goes through that to toggle the controls mentioned above, meaning by default these are not toggleable
 */
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

    var CAMERA_SPEED = 1

    val view = GUILevelView(rootChild, name,
            { 0 }, { 0 },
            { this.widthPixels }, { this.heightPixels },
            camera, zoomLevel, open)
    val outline = GUIOutline(view, name + " outline", open = open)
    val nameText = GUIText(view, name, 1, 1, name, layer = 1)
    val controls = mutableListOf<GUIElement>()

    init {
        InputManager.registerControlPressHandler(this, ControlPressHandlerType.SCREEN_THIS, Control.UP, Control.DOWN, Control.LEFT, Control.RIGHT)
        nameText.transparentToInteraction = true
        controls.add(generateDimensionDragGrip(2, 2))
        controls.add(generateDragGrip(2))
        controls.add(generateCloseButton(2))
        controls.add(nameText)
        partOfLevel = true
    }

    override fun handleControlPress(p: ControlPress) {
        if (p.pressType == PressType.RELEASED || camera !is MovingObject)
            return
        val c = p.control
        val m = camera as MovingObject
        if (c == Control.UP) {
            m.yVel -= CAMERA_SPEED
        } else if (c == Control.DOWN) {
            m.yVel += CAMERA_SPEED
        } else if (c == Control.RIGHT) {
            m.xVel += CAMERA_SPEED
        } else if (c == Control.LEFT) {
            m.xVel -= CAMERA_SPEED
        }
    }
}