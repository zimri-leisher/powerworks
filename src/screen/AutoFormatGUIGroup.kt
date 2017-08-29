package screen

class AutoFormatGUIGroup(parent: RootGUIElement? = RootGUIElementObject,
                         name: String,
                         relXPixel: Int, relYPixel: Int,
                         layer: Int = (parent?.layer ?: 0) + 1, val yPixelSeparation: Int = 0) :
        GUIGroup(parent, name, relXPixel, relYPixel, layer) {

    var nextYPixel = 0

    override fun onAddChild(child: GUIElement) {
        child.relYPixel = nextYPixel
        child.relXPixel = 0
        nextYPixel += child.heightPixels + yPixelSeparation
        super.onAddChild(child)
    }
}