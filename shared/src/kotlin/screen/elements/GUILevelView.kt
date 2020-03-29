package screen.elements

import com.badlogic.gdx.graphics.OrthographicCamera
import graphics.Renderer
import level.LevelManager
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

    val level get() = camera.level

    /**
     * A list of objects that want to be informed when the camera of this view moves
     */
    val moveListeners = mutableListOf<CameraMovementListener>()

    /**
     * The level object that this view will follow. The view rectangle will be centered on this but clamped to inside the level
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
     * Higher values means a higher [zoomMultiplier]
     */
    var zoomLevel = zoomLevel
        set(value) {
            field = value
            zoomMultiplier = value * ZOOM_INCREMENT
            updateCamera()
            updateViewRectangle()
        }

    /**
     * The current view rectangle. It will be clamped to the width and height of the level, so that you are never able to see out of bounds
     */
    var viewRectangle = Rectangle()

    init {
        if (camera is MovingObject) {
            camera.moveListeners.add(this)
        }
        updateCamera()
        updateViewRectangle()
        LevelManager.addLevelView(this)
    }

    private fun updateCamera() {
        cameraMatrix.viewportWidth = widthPixels.toFloat() * Game.SCALE
        cameraMatrix.viewportHeight = heightPixels.toFloat() * Game.SCALE
        cameraMatrix.zoom = 1 / zoomMultiplier
        cameraMatrix.position.apply {
            var viewXPixel = camera.xPixel + camera.hitbox.xStart + camera.hitbox.width / 2
            var viewYPixel = camera.yPixel + camera.hitbox.yStart + camera.hitbox.height / 2
            if (viewXPixel - viewWidthPixels / 2 < 0) {
                viewXPixel = viewWidthPixels / 2
            } else if (viewXPixel + viewWidthPixels / 2 > camera.level.widthPixels) {
                viewXPixel = camera.level.widthPixels - viewWidthPixels / 2
            }
            if (viewYPixel - viewHeightPixels / 2 < 0) {
                viewYPixel = viewHeightPixels / 2
            } else if (viewYPixel + viewHeightPixels / 2 > camera.level.heightPixels) {
                viewYPixel = camera.level.heightPixels - viewHeightPixels / 2
            }
            x = (viewXPixel * Game.SCALE).toFloat()
            y = (viewYPixel * Game.SCALE).toFloat()
        }
        moveListeners.forEach { it.onCameraMove(this, camera.xPixel, camera.yPixel) }
        cameraMatrix.update()
    }

    private fun updateViewRectangle() {
        viewWidthPixels = (widthPixels / zoomMultiplier).toInt()
        viewHeightPixels = (heightPixels / zoomMultiplier).toInt()
        var viewXPixel = (camera.xPixel + camera.hitbox.xStart + camera.hitbox.width / 2) - viewWidthPixels / 2
        var viewYPixel = (camera.yPixel + camera.hitbox.yStart + camera.hitbox.height / 2) - viewHeightPixels / 2
        if (viewXPixel < 0) {
            viewXPixel = 0
        } else if (viewXPixel + viewWidthPixels > camera.level.widthPixels) {
            viewXPixel = camera.level.widthPixels - viewWidthPixels
        }
        if (viewYPixel < 0) {
            viewYPixel = 0
        } else if (viewYPixel + viewHeightPixels > camera.level.heightPixels) {
            viewYPixel = camera.level.heightPixels - viewHeightPixels
        }
        viewRectangle = Rectangle(viewXPixel, viewYPixel, viewWidthPixels, viewHeightPixels)
    }

    override fun onDimensionChange(oldWidth: Int, oldHeight: Int) {
        updateCamera()
        updateViewRectangle()
        LevelManager.updateLevelAndViewInformation()
    }

    override fun onPositionChange(pXPixel: Int, pYPixel: Int) {
        updateCamera()
        LevelManager.updateLevelAndViewInformation()
    }

    //Camera moves
    override fun onMove(m: MovingObject, pXPixel: Int, pYPixel: Int) {
        if (open) {
            updateCamera()
            updateViewRectangle()
            moveListeners.forEach { it.onCameraMove(this, pXPixel, pYPixel) }
        }
    }

    override fun render() {
        val screenMatrix = Renderer.batch.projectionMatrix.cpy()
        Renderer.batch.projectionMatrix = cameraMatrix.combined
        Renderer.setClip(xPixel, yPixel, widthPixels, heightPixels)
        camera.level.render(this)
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