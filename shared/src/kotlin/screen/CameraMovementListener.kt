package screen

import screen.element.ElementLevelView

/**
 * To add a class to the list of listeners, find the GUILevelView (usually inside of a LevelViewWindow) and add it to moveListeners
 */
interface CameraMovementListener {
    fun onCameraMove(view: ElementLevelView, prevX: Int, prevY: Int)
}