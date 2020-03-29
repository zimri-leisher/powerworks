package screen

import io.*
import level.LevelObject
import level.moving.MovingObject
import screen.elements.*

/**
 * A window for interaction with the level.
 * Has a GUILevelView, a GUIOutline around it, some miscellaneous controls for movement, resizing, etc.
 * By default, the IngameGUI has 4 of these. Control.TOGGLE_VIEW_CONTROLS goes through that to toggle the controls mentioned above
 */
class LevelViewWindow(name: String,
                      xPixel: Int, yPixel: Int,
                      widthPixels: Int, heightPixels: Int,
                      var camera: LevelObject,
                      zoomLevel: Int = 10,
                      open: Boolean = false,
                      layer: Int = 0,
                      windowGroup: WindowGroup) :
        GUIWindow(name, xPixel, yPixel, widthPixels, heightPixels, windowGroup, open, layer),
        ControlPressHandler {

    var CAMERA_SPEED = 1

    val view = GUILevelView(this, name,
            { 0 }, { 0 },
            { this.widthPixels }, { this.heightPixels },
            camera, zoomLevel, open)
    val outline = GUIOutline(view, name + " outline", open = open)
    val nameText = GUIText(view, name, 1, 1, name, layer = 1)
    private val controls = mutableListOf<GUIElement>()

    init {
        InputManager.registerControlPressHandler(this, ControlPressHandlerType.SCREEN_THIS, Control.CAMERA_UP, Control.CAMERA_DOWN, Control.CAMERA_LEFT, Control.CAMERA_RIGHT)
        nameText.transparentToInteraction = true
        controls.add(generateDimensionDragGrip(2))
        controls.add(generateDragGrip(2))
        controls.add(generateCloseButton(2))
        controls.add(nameText)
        controls.forEach { it.matchParentOpening = false; it.open = false }
    }

    override fun handleControlPress(p: ControlPress) {
        // eventually check if this was the last selected gui level view
        if (p.control in Control.Group.CAMERA && p.pressType != PressType.RELEASED && camera is MovingObject) {
            val c = p.control
            val m = camera as MovingObject
            if (c == Control.CAMERA_UP) {
                m.yVel += CAMERA_SPEED
            } else if (c == Control.CAMERA_DOWN) {
                m.yVel -= CAMERA_SPEED
            } else if (c == Control.CAMERA_RIGHT) {
                m.xVel += CAMERA_SPEED
            } else if (c == Control.CAMERA_LEFT) {
                m.xVel -= CAMERA_SPEED
            }
        } else if(p.control == Control.TOGGLE_VIEW_CONTROLS && p.pressType == PressType.PRESSED) {
            controls.forEach { it.open = !it.open }
        }
    }
}