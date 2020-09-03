package screen.element

import com.badlogic.gdx.graphics.OrthographicCamera
import graphics.Renderer
import graphics.TextureRenderParams
import io.*
import level.LevelManager
import level.LevelObject
import level.MovementListener
import level.moving.MovingObject
import main.DebugCode
import main.Game
import screen.CameraMovementListener
import screen.gui.GuiElement
import screen.Interaction
import screen.gui.GuiDebugInfo
import java.awt.Rectangle

class ElementLevelView(parent: GuiElement, camera: LevelObject) : GuiElement(parent), MovementListener, ControlEventHandler {
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

    private val cameraMatrix = OrthographicCamera(Game.WIDTH.toFloat() * Game.SCALE, Game.HEIGHT.toFloat() * Game.SCALE)

    /**
     * The zoom 'level'. This is different from a multiplier in that it is an integer value that determines the multiplier.
     * Higher values means a higher [zoomMultiplier]
     */
    var zoomLevel = 10
        set(value) {
            field = value
            zoomMultiplier = value * ZOOM_INCREMENT
            updateCamera()
            updateViewRectangle()
        }

    /**
     * The viewWidth/HeightPixels multiplier
     */
    var zoomMultiplier = zoomLevel * ZOOM_INCREMENT
        private set

    private var viewWidthPixels = (widthPixels / zoomMultiplier).toInt()

    private var viewHeightPixels = (heightPixels / zoomMultiplier).toInt()

    /**
     * The current view rectangle. It will be clamped to the width and height of the level, so that you are never able to see out of bounds
     */
    var viewRectangle = Rectangle()

    init {
        InputManager.register(this, Control.CAMERA_UP, Control.CAMERA_DOWN, Control.CAMERA_LEFT, Control.CAMERA_RIGHT)
        if (camera is MovingObject) {
            camera.moveListeners.add(this)
        }
        updateCamera()
        updateViewRectangle()
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
        cameraMatrix.update()
        moveListeners.forEach { it.onCameraMove(this, camera.xPixel, camera.yPixel) }
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

    override fun onChangeDimensions() {
        updateCamera()
        updateViewRectangle()
        super.onChangeDimensions()
    }

    override fun onChangePlacement() {
        updateCamera()
        super.onChangePlacement()
    }

    override fun onMove(m: MovingObject, pXPixel: Int, pYPixel: Int) {
        if (open) {
            updateCamera()
            updateViewRectangle()
            moveListeners.forEach { it.onCameraMove(this, pXPixel, pYPixel) }
        }
    }

    override fun onInteractOn(interaction: Interaction) {
        if(interaction.event.type == ControlEventType.PRESS || interaction.event.type == ControlEventType.HOLD) {
            if (interaction.event.control == Control.SCROLL_DOWN) {
                if (zoomLevel + 1 <= MIN_ZOOM)
                    zoomLevel++
            } else if (interaction.event.control == Control.SCROLL_UP) {
                if (zoomLevel - 1 >= MAX_ZOOM)
                    zoomLevel--
            }
        }
        super.onInteractOn(interaction)
    }

    override fun render(params: TextureRenderParams?) {
        if(Game.currentDebugCode == DebugCode.LEVEL_INFO) {
            GuiDebugInfo.show(this, listOf("camera=$camera", "level=${camera.level.id}"))
        }
        val screenMatrix = Renderer.batch.projectionMatrix.cpy()
        Renderer.batch.projectionMatrix = cameraMatrix.combined
        Renderer.pushClip(xPixel, yPixel, widthPixels, heightPixels)
        camera.level.render(this)
        Renderer.batch.projectionMatrix = screenMatrix
        Renderer.popClip()
        super.render(params)
    }

    override fun handleControlEvent(event: ControlEvent) {
        if(!open || LevelManager.levelViewUnderMouse != this) {
            return
        }
        if (event.control in Control.Group.CAMERA && event.type != ControlEventType.RELEASE && camera is MovingObject) {
            val c = event.control
            val m = camera as MovingObject
            if (c == Control.CAMERA_UP) {
                m.yVel += m.type.maxSpeed
            } else if (c == Control.CAMERA_DOWN) {
                m.yVel -= m.type.maxSpeed
            } else if (c == Control.CAMERA_RIGHT) {
                m.xVel += m.type.maxSpeed
            } else if (c == Control.CAMERA_LEFT) {
                m.xVel -= m.type.maxSpeed
            }
        }
    }

    companion object {
        const val ZOOM_INCREMENT = 0.1f
        const val MAX_ZOOM = 4
        const val MIN_ZOOM = 25
    }
}