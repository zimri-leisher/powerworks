package screen.mouse.tool

import behavior.Behavior
import behavior.BehaviorTree
import graphics.Image
import graphics.Renderer
import io.Control
import io.ControlEvent
import io.ControlEventType
import level.Level
import level.LevelManager
import level.LevelPosition
import level.entity.Entity
import level.entity.EntityGroup
import main.heightPixels
import main.widthPixels
import misc.Geometry
import misc.PixelCoord
import network.MovingObjectReference
import player.ActionControlEntity
import player.PlayerManager
import screen.mouse.Mouse
import kotlin.math.PI
import kotlin.math.atan2

object EntityController : Tool(Control.USE_ENTITY_COMMAND) {

    const val DRAG_DISTANCE_BEFORE_MENU_OPEN = 8

    var startXPixel = 0
    var startYPixel = 0
    var startLevelXPixel = 0
    var startLevelYPixel = 0
    var active = false

    var currentlyHoveringCommand: BehaviorTree? = null
    var selectedCommand: BehaviorTree? = null

    override fun onUse(event: ControlEvent, mouseLevelXPixel: Int, mouseLevelYPixel: Int): Boolean {
        if (event.control == Control.USE_ENTITY_COMMAND) {
            if (event.type == ControlEventType.PRESS) {
                startXPixel = Mouse.xPixel
                startYPixel = Mouse.yPixel
                startLevelXPixel = LevelManager.mouseLevelXPixel
                startLevelYPixel = LevelManager.mouseLevelYPixel
            } else if (event.type == ControlEventType.HOLD) { // TODO should be if held
                if (!active && Geometry.distance(Mouse.xPixel, Mouse.yPixel, startXPixel, startYPixel) > DRAG_DISTANCE_BEFORE_MENU_OPEN) {
                    active = true
                }
                if (active) {
                    val angle = atan2(Mouse.yPixel - startYPixel.toDouble(), Mouse.xPixel - startXPixel.toDouble())
                    if (angle > 3 * PI / 4 || angle < -3 * PI / 4) {
                        // left
                        currentlyHoveringCommand = Behavior.Offense.ATTACK_ARG
                    } else if (angle <= 3 * PI / 4 && angle >= PI / 4) {
                        // top
                        currentlyHoveringCommand = Behavior.Movement.PATH_TO_FORMATION
                    } else if (angle < PI / 4 && angle >= -PI / 4) {
                        // right
                        currentlyHoveringCommand = null // should be stop
                    } else {
                        // bottom
                        currentlyHoveringCommand = null // should be defend
                    }
                }
            } else {
                if (active) {
                    selectedCommand = currentlyHoveringCommand
                    println("selected $selectedCommand")
                    active = false
                }
                if (!Selector.dragging) {
                    if (selectedCommand != null && !active) {
                        controlEntities(selectedCommand!!)
                        return true
                    }
                }
            }
        }
        return false
    }

    override fun renderAbove(level: Level) {
        if(level == LevelManager.levelUnderMouse) {
            if (active) {
                Renderer.renderTexture(Image.Gui.ENTITY_CONTROLLER_MENU2, startLevelXPixel - Image.Gui.ENTITY_CONTROLLER_MENU2.widthPixels / 2, startLevelYPixel - Image.Gui.ENTITY_CONTROLLER_MENU2.heightPixels / 2)
            }
        }
    }

    private fun controlEntities(command: BehaviorTree) {
        when (command) {
            Behavior.Movement.PATH_TO_FORMATION -> {
                for (group in EntityGroup.ALL) {
                    if (group.formation != null) {
                        val boundaries = group.formation!!.boundaries
                        if (Geometry.contains(boundaries.x, boundaries.y, boundaries.width, boundaries.height,
                                        LevelManager.mouseLevelXPixel, LevelManager.mouseLevelYPixel, 0, 0)) {
                            // TODO merge groups
                        }
                    }
                }
                PlayerManager.takeAction(ActionControlEntity(PlayerManager.localPlayer,
                        Selector.currentSelected.filterIsInstance<Entity>().map { it.toReference() as MovingObjectReference },
                        command, LevelPosition(LevelManager.mouseLevelXPixel, LevelManager.mouseLevelYPixel, LevelManager.levelUnderMouse!!)))
            }
            Behavior.Offense.ATTACK_ARG -> {
                PlayerManager.takeAction(ActionControlEntity(PlayerManager.localPlayer,
                        Selector.currentSelected.filterIsInstance<Entity>().map { it.toReference() as MovingObjectReference },
                        command, LevelManager.levelObjectUnderMouse?.toReference()))
            }
        }
    }
}