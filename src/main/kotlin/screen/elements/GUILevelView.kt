package screen.elements

import com.badlogic.gdx.graphics.OrthographicCamera
import graphics.Renderer
import level.LevelObject
import level.MovementListener
import level.moving.MovingObject
import main.Game
import screen.CameraMovementListener
import java.awt.Rectangle

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
        private set

    private var viewWidthPixels = (widthPixels / zoomMultiplier).toInt()

    private var viewHeightPixels = (heightPixels / zoomMultiplier).toInt()

    val libgdxCamera = OrthographicCamera(Game.WIDTH.toFloat() * Game.SCALE, Game.HEIGHT.toFloat() * Game.SCALE)

    var zoomLevel = zoomLevel
        set(value) {
            field = value
            zoomMultiplier = value * ZOOM_INCREMENT
            updateView()
            onCameraMove(camera.xPixel, camera.yPixel)
        }

    var viewRectangle: Rectangle

    init {
        // Only one level loaded at a time so no need for parents
        if (camera is MovingObject) {
            camera.moveListeners.add(this)
        }
        onCameraMove(camera.xPixel, camera.yPixel)
        viewRectangle = Rectangle(camera.xPixel - viewWidthPixels / 2, camera.yPixel - viewHeightPixels / 2, viewWidthPixels, viewHeightPixels)
    }

    override fun onClose() {
        Game.currentLevel.openViews.remove(this)
    }

    override fun onOpen() {
        Game.currentLevel.openViews.add(this)
    }

    private fun updateView() {
        viewWidthPixels = (widthPixels / zoomMultiplier).toInt()
        viewHeightPixels = (heightPixels / zoomMultiplier).toInt()
        viewRectangle = Rectangle(camera.xPixel - viewWidthPixels / 2, camera.yPixel - viewHeightPixels / 2, viewWidthPixels, viewHeightPixels)
        libgdxCamera.zoom = 1 / zoomMultiplier
        libgdxCamera.update()
    }

    private fun onCameraMove(pXPixel: Int, pYPixel: Int) {
        libgdxCamera.position.apply {
            x = camera.xPixel.toFloat() * Game.SCALE
            y = camera.yPixel.toFloat() * Game.SCALE
        }
        libgdxCamera.update()
        viewRectangle = Rectangle(camera.xPixel - viewWidthPixels / 2, camera.yPixel - viewHeightPixels / 2, viewWidthPixels, viewHeightPixels)
        moveListeners.forEach { it.onCameraMove(this, pXPixel, pYPixel) }
    }

    override fun onDimensionChange(oldWidth: Int, oldHeight: Int) {
        libgdxCamera.viewportWidth = Game.WIDTH.toFloat() * Game.SCALE
        libgdxCamera.viewportHeight = Game.HEIGHT.toFloat() * Game.SCALE
        updateView()
        Game.currentLevel.updateViewBeingInteractedWith()
    }

    override fun onPositionChange(pXPixel: Int, pYPixel: Int) {
        Game.currentLevel.updateViewBeingInteractedWith()
    }

    //Camera moves
    override fun onMove(m: MovingObject, pXPixel: Int, pYPixel: Int) {
        if (open)
            onCameraMove(pXPixel, pYPixel)
    }

    override fun render() {
        val screenMatrix = Renderer.batch.projectionMatrix.cpy()
        Renderer.batch.projectionMatrix = libgdxCamera.combined
        Game.currentLevel.render(this)
        Renderer.batch.projectionMatrix = screenMatrix
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