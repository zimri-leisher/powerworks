package screen

class AutoFormatGUIGroup(parent: RootGUIElement,
                         name: String,
                         relXPixel: Int, relYPixel: Int,
                         open: Boolean = false,
                         layer: Int = parent.layer + 1, val yPixelSeparation: Int = 0) :
        GUIGroup(parent, name, relXPixel, relYPixel, open, layer) {

    var nextYPixel = 0

    override fun onAddChild(child: GUIElement) {
        child.relYPixel = nextYPixel
        child.relXPixel = 0
        child.layer = layer + 1
        nextYPixel += child.heightPixels + yPixelSeparation
        super.onAddChild(child)
    }
}