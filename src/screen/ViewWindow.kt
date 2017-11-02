package screen

import level.LevelObject

class ViewWindow(name: String,
                 xPixel: Int, yPixel: Int,
                 widthPixels: Int, heightPixels: Int,
                 camera: LevelObject,
                 zoomLevel: Int = 10,
                 open: Boolean = false,
                 layer: Int = 0,
                 windowGroup: WindowGroup) :
        GUIWindow(name, xPixel, yPixel, widthPixels, heightPixels, open, layer, windowGroup) {
    val view = GUIView(this.rootChild, name,
            0, 0,
            widthPixels, heightPixels,
            camera, zoomLevel, open, layer)
    val outline = GUIOutline(view, name + " outline", open = open)
    val dragGrip = generateDragGrip(rootChild, view.layer + 2)
    val closeButton = generateCloseButton(rootChild, view.layer + 2)
    val nameText = GUIText(view, name, 1, 4, name, layer = view.layer + 1)

    init {
        nameText.transparentToInteraction = true
    }
}