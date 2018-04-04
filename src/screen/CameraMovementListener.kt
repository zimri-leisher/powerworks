package screen

import screen.elements.GUILevelView

/**
 * To add a class to the list of listeners, find the GUILevelView (usually inside of a ViewWindow) and add it to moveListeners
 */
interface CameraMovementListener {
    fun onCameraMove(view: GUILevelView, pXPixel: Int, pYPixel: Int)
}