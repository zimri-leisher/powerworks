package screen

import screen.elements.GUIView

interface CameraMovementListener {
    fun onCameraMove(view: GUIView, pXPixel: Int, pYPixel: Int)
}