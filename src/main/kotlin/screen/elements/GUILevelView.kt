package screen.elements

import com.badlogic.gdx.graphics.OrthographicCamera
import graphics.Renderer
import level.LevelObject
import level.MovementListener
import level.moving.MovingObject
import main.Game
import screen.CameraMovementListener
import java.awt.Rectangle

/**
 * A 'window' (in the non-GUI sense) into the level. When opened, this will render the level around the given camera.
 * Scroll on to zoom in or out, which decreases and increases the width and height of the view, respectively
 */
class GUILevelView(parent: RootGUIElement,
                   name: String,
                   xAlignment: Alignment, yAlignment: Alignment,
                   widthAlignment: Alignment, heightAlignment: Alignment,
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

    /**
     * A list of objects that want to be informed when the camera of this view moves
     */
    val moveListeners = mutableListOf<CameraMovementListener>()

    /**
     * The level object that this view will follow. The view rectangle will be centered on this, meaning the bottom
     * left corner is at (camera.xPixel - viewWidthPixels / 2, camera.yPixel - viewHeightPixels / 2)
     */
    var camera = camera
        set(value) {
            val old = field
            if (old is MovingObject)
                old.moveListeners.remove(this)
            field = value
            if (value is MovingObject)
                value.moveListeners.add(this)
            if (open) {
                updateCamera()
            }
        }

    /**
     * The viewWidth/HeightPixels multiplier
     */
    var zoomMultiplier = zoomLevel * ZOOM_INCREMENT
        private set

    private var viewWidthPixels = (widthPixels / zoomMultiplier).toInt()

    private var viewHeightPixels = (heightPixels / zoomMultiplier).toInt()

    private val cameraMatrix = OrthographicCamera(Game.WIDTH.toFloat() * Game.SCALE, Game.HEIGHT.toFloat() * Game.SCALE)

    /**
     * The zoom 'level'. This is different from a multiplier in that it is an integer value that determines the multiplier.
     * Higher values means a higher zoomMultiplier
     */
    var zoomLevel = zoomLevel
        set(value) {
            field = value
            zoomMultiplier = value * ZOOM_INCREMENT
            updateCamera()
            updateViewRectangle()
        }

    /**
     * The current view rectangle. Bottom left corner is at (camera.xPixel - viewWidthPixels / 2, camera.yPixel - viewHeightPixels / 2),
     * width is viewWidthPixels, height is viewHeightPixels
     */
    var viewRectangle: Rectangle

    init {
        // Only one level loaded at a time so no need for parents
        if (camera is MovingObject) {
            camera.moveListeners.add(this)
        }
        updateCamera()
        viewRectangle = Rectangle(camera.xPixel - viewWidthPixels / 2, camera.yPixel - viewHeightPixels / 2, viewWidthPixels, viewHeightPixels)
    }

    override fun onClose() {
        Game.currentLevel.openViews.remove(this)
    }

    override fun onOpen() {
        Game.currentLevel.openViews.add(this)
    }

    private fun updateCamera() {
        cameraMatrix.viewportWidth = Game.WIDTH.toFloat() * Game.SCALE
        cameraMatrix.viewportHeight = Game.HEIGHT.toFloat() * Game.SCALE
        cameraMatrix.zoom = 1 / zoomMultiplier
        cameraMatrix.position.apply {
            x = ((camera.xPixel - this@GUILevelView.xPixel + (Game.WIDTH - this@GUILevelView.widthPixels) / 2) * Game.SCALE).toFloat()
            y = ((camera.yPixel - this@GUILevelView.yPixel + (Game.HEIGHT - this@GUILevelView.heightPixels) / 2) * Game.SCALE).toFloat()
        }
        cameraMatrix.update()
    }

    private fun updateViewRectangle() {
        viewWidthPixels = (widthPixels / zoomMultiplier).toInt()
        viewHeightPixels = (heightPixels / zoomMultiplier).toInt()
        viewRectangle = Rectangle(camera.xPixel - viewWidthPixels / 2, camera.yPixel - viewHeightPixels / 2, viewWidthPixels, viewHeightPixels)
    }

    override fun onDimensionChange(oldWidth: Int, oldHeight: Int) {
        updateCamera()
        updateViewRectangle()
        Game.currentLevel.updateViewBeingInteractedWith()
    }

    override fun onPositionChange(pXPixel: Int, pYPixel: Int) {
        updateCamera()
        Game.currentLevel.updateViewBeingInteractedWith()
    }

    //Camera moves
    override fun onMove(m: MovingObject, pXPixel: Int, pYPixel: Int) {
        if (open) {
            updateCamera()
            viewRectangle = Rectangle(camera.xPixel - viewWidthPixels / 2, camera.yPixel - viewHeightPixels / 2, viewWidthPixels, viewHeightPixels)
            moveListeners.forEach { it.onCameraMove(this, pXPixel, pYPixel) }
        }
    }

    override fun render() {
        val screenMatrix = Renderer.batch.projectionMatrix.cpy()
        Renderer.batch.projectionMatrix = cameraMatrix.combined
        Renderer.setClip(xPixel, yPixel, widthPixels, heightPixels)
        Game.currentLevel.render(this)
        Renderer.batch.projectionMatrix = screenMatrix
        Renderer.resetClip()
    }

    override fun onScroll(dir: Int) {
        if (dir == -1) {
            if (zoomLevel + 1 <= MIN_ZOOM)
                zoomLevel++
        } else if (dir == 1) {
            if (zoomLevel - 1 >= MAX_ZOOM)
                zoomLevel--
        }
    }

    companion object {
        const val ZOOM_INCREMENT = 0.1f
        const val MAX_ZOOM = 4
        const val MIN_ZOOM = 25
    }
}