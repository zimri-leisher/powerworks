package screen

import graphics.Renderer
import level.CameraObject
import main.Game
import java.awt.Rectangle
import java.awt.Transparency
import java.awt.image.VolatileImage

class GUIView(parent: RootGUIElement? = RootGUIElementObject,
              name: String,
              relXPixel: Int, relYPixel: Int,
              widthPixels: Int, heightPixels: Int,
              zoomLevel: Int = 10,
              var camera: CameraObject) : GUIElement(parent, name, relXPixel, relYPixel, widthPixels, heightPixels) {

    private var zoomMultiplier = zoomLevel * ZOOM_INCREMENT

    private var viewWidthPixels = (widthPixels / zoomMultiplier).toInt()

    private var viewHeightPixels = (heightPixels / zoomMultiplier).toInt()

    override var widthPixels = widthPixels
        set(value) {
            field = value
            updateView()
        }
    override var heightPixels = heightPixels
        set(value) {
            field = value
            updateView()
        }
    var zoomLevel = zoomLevel
        set(value) {
            field = value
            zoomMultiplier = value * ZOOM_INCREMENT
            updateView()
        }


    var buffer = Game.graphicsConfiguration.createCompatibleVolatileImage(viewWidthPixels * Game.SCALE, viewHeightPixels * Game.SCALE, Transparency.TRANSLUCENT)

    init {
        DebugOverlay.setInfo(name + " zoom level", zoomLevel.toString())
        DebugOverlay.setInfo(name + " dimensions", "width: $viewWidthPixels, height: $viewHeightPixels")
    }

    private fun updateView() {
        viewWidthPixels = (widthPixels / zoomMultiplier).toInt()
        viewHeightPixels = (heightPixels / zoomMultiplier).toInt()
        DebugOverlay.setInfo(name + " dimensions", "width: $viewWidthPixels, height: $viewHeightPixels")
        buffer.flush()
        buffer = Game.graphicsConfiguration.createCompatibleVolatileImage(viewWidthPixels * Game.SCALE, viewHeightPixels * Game.SCALE, Transparency.TRANSLUCENT)
    }

    override fun update() {

    }

    fun getViewRectangle(): Rectangle {
        return Rectangle(camera.xPixel - viewWidthPixels / 2, camera.yPixel - viewHeightPixels / 2, viewWidthPixels, viewHeightPixels)
    }

    override fun render() {
        val oldG2D = Renderer.g2d
        do {
            if (buffer.validate(Game.graphicsConfiguration) == VolatileImage.IMAGE_INCOMPATIBLE)
                updateView()
            Renderer.g2d = buffer.createGraphics()
            Renderer.xPixelOffset = -(camera.xPixel - viewWidthPixels / 2)
            Renderer.yPixelOffset = -(camera.yPixel - viewHeightPixels / 2)
            Game.currentLevel.render(this)
            //Level.render
            //    Tile.render
            //    split entities into x groups, sorted by lowest y first, one between the middle of each tile, y coordinate wise
            //    starting from the bottom of the lowest y group (coordinate wise):
            //        render necessary line of blocks that would be below it (block stores 4 pts, if closest point is within view rectangle render it?)
            //        render entities group in order
            Renderer.g2d.dispose()
            Renderer.g2d = oldG2D
            Renderer.xPixelOffset = 0
            Renderer.yPixelOffset = 0
            oldG2D.drawImage(buffer, xPixel * Game.SCALE, yPixel * Game.SCALE, widthPixels * Game.SCALE, heightPixels * Game.SCALE, null)
        } while (buffer.contentsLost())

    }

    override fun onMouseScroll(dir: Int) {
        if (dir == -1) {
            if (zoomLevel + 1 <= MIN_ZOOM)
                zoomLevel++
        } else if (dir == 1) {
            if (zoomLevel - 1 >= MAX_ZOOM)
                zoomLevel--
        }
        DebugOverlay.setInfo(name + " zoom level", zoomLevel.toString())
    }

    companion object {
        const val ZOOM_INCREMENT = 0.1
        const val MAX_ZOOM = 4
        const val MIN_ZOOM = 25
    }
}