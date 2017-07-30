package screen

import level.CameraObject

class GUIView(parent: RootGUIElement? = RootGUIElementObject,
              name: String,
              relXPixel: Int, relYPixel: Int,
              widthPixels: Int, heightPixels: Int,
              camera: CameraObject) : GUIElement(parent, name, relXPixel, relXPixel, widthPixels, heightPixels) {

    override fun update() {

    }

    override fun render() {
        //Level.render
        //    Tile.render
        //    split entities into x groups, sorted by lowest y first, one between the middle of each tile, y coordinate wise
        //    starting from the bottom of the lowest y group (coordinate wise):
        //        render necessary line of blocks that would be below it (block stores 4 pts, if closest point is within view rectangle render it?)
        //        render entities group in order
    }
}