package screen

import graphics.Renderer
import io.PressType
import level.LevelObject
import level.MovementListener
import level.moving.MovingObject
import main.Game
import java.awt.Rectangle
import java.awt.Transparency
import java.awt.image.VolatileImage

class GUIView(parent: RootGUIElement,
              name: String,
              xAlignment: () -> Int, yAlignment: () -> Int,
              widthAlignment: () -> Int, heightAlignment: () -> Int,
              camera: LevelObject, zoomLevel: Int = 10,
              open: Boolean = false,
              layer: Int = parent.layer + 1) :
        GUIElement(parent, name, xAlignment, yAlignment, widthAlignment, heightAlignment, open, layer), MovementListener {

    constructor(parent: RootGUIElement,
                name: String,
                relXPixel: Int, relYPixel: Int,
                widthPixels: Int, heightPixels: Int,
                camera: LevelObject, zoomLevel: Int = 10,
                open: Boolean = false,
                layer: Int = parent.layer + 1) :
            this(parent, name, { relXPixel }, { relYPixel }, { widthPixels }, { heightPixels }, camera, zoomLevel, open, layer)

    val moveListeners = mutableListOf<CameraMovementListener>()

    var camera = camera
        set(value) {
            val old = field
            if (old is MovingObject)
                old.moveListeners.remove(this)
            field = value
            if (value is MovingObject)
                value.moveListeners.add(this)
            if (open) {
                onCameraMove(old.xPixel, old.yPixel)
            }
        }

    var zoomMultiplier = zoomLevel * ZOOM_INCREMENT

    private var viewWidthPixels = (widthPixels / zoomMultiplier).toInt()

    private var viewHeightPixels = (heightPixels / zoomMultiplier).toInt()

    var zoomLevel = zoomLevel
        set(value) {
            field = value
            zoomMultiplier = value * ZOOM_INCREMENT
            updateView()
            onCameraMove(camera.xPixel, camera.yPixel)
        }

    var currentBuffer = Game.graphicsConfiguration.createCompatibleVolatileImage(viewWidthPixels * Game.SCALE, viewHeightPixels * Game.SCALE, Transparency.TRANSLUCENT)

    lateinit var pregeneratedBuffers: Array<VolatileImage>

    var viewRectangle: Rectangle

    init {
        DebugOverlay.setInfo(name + " zoom level", zoomLevel.toString())
        DebugOverlay.setInfo(name + " dimensions", "width: $viewWidthPixels, height: $viewHeightPixels")
        // Only one level loaded at a time so no need for parents
        if (camera is MovingObject) {
            camera.moveListeners.add(this)
        }
        if (open)
            Game.currentLevel.views.add(this)
        viewRectangle = Rectangle(camera.xPixel - viewWidthPixels / 2, camera.yPixel - viewHeightPixels / 2, viewWidthPixels, viewHeightPixels)
        fillPregenBuffers()
    }

    override fun onClose() {
        Game.currentLevel.views.remove(this)
    }

    override fun onOpen() {
        Game.currentLevel.views.add(this)
    }

    /**
     * Generates VolatileImages for each possible zoom level at once. Flushing (NOTE: FLUSHING IS WHAT CAUSED THE HUGE MEMORY SPIKES. NEEDS INVESTIGATION) and validating are handled in updateView
     */
    private fun fillPregenBuffers() {
        val g = arrayOfNulls<VolatileImage>(MIN_ZOOM - MAX_ZOOM + 1)
        for (i in MAX_ZOOM..MIN_ZOOM) {
            g[i - MAX_ZOOM] = Game.graphicsConfiguration.createCompatibleVolatileImage((widthPixels / (i * ZOOM_INCREMENT)).toInt() * Game.SCALE, (heightPixels / (i * ZOOM_INCREMENT)).toInt() * Game.SCALE, Transparency.TRANSLUCENT)
        }
        pregeneratedBuffers = g.requireNoNulls()
    }

    private fun updateView() {
        viewWidthPixels = (widthPixels / zoomMultiplier).toInt()
        viewHeightPixels = (heightPixels / zoomMultiplier).toInt()
        pregeneratedBuffers[zoomLevel - MAX_ZOOM].validate(Game.graphicsConfiguration)
        currentBuffer = pregeneratedBuffers[zoomLevel - MAX_ZOOM]
        viewRectangle = Rectangle(camera.xPixel - viewWidthPixels / 2, camera.yPixel - viewHeightPixels / 2, viewWidthPixels, viewHeightPixels)
    }

    override fun update() {
    }

    private fun onCameraMove(pXPixel: Int, pYPixel: Int) {
        viewRectangle = Rectangle(camera.xPixel - viewWidthPixels / 2, camera.yPixel - viewHeightPixels / 2, viewWidthPixels, viewHeightPixels)
        moveListeners.forEach { it.onCameraMove(this, pXPixel, pYPixel) }
    }

    override fun onDimensionChange(oldWidth: Int, oldHeight: Int) {
        fillPregenBuffers()
        updateView()
    }

    //Camera moves
    override fun onMove(m: MovingObject, pXPixel: Int, pYPixel: Int) {
        if (open)
            onCameraMove(pXPixel, pYPixel)
    }

    override fun render() {
        val oldG2D = Renderer.g2d
        do {
            if (currentBuffer.validate(Game.graphicsConfiguration) == VolatileImage.IMAGE_INCOMPATIBLE)
                updateView()
            Renderer.g2d = currentBuffer.createGraphics()
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
            oldG2D.drawImage(currentBuffer, xPixel * Game.SCALE, yPixel * Game.SCALE, widthPixels * Game.SCALE, heightPixels * Game.SCALE, null)
        } while (currentBuffer.contentsLost())
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

    override fun onMouseActionOn(type: PressType, xPixel: Int, yPixel: Int, button: Int) {
        Game.currentLevel.onMouseAction(type, xPixel, yPixel)
    }

    companion object {
        const val ZOOM_INCREMENT = 0.1
        const val MAX_ZOOM = 4
        const val MIN_ZOOM = 25
    }
}