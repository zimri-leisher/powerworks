package screen

interface CameraMovementListener {
    fun onCameraMove(view: GUIView, pXPixel: Int, pYPixel: Int)
}