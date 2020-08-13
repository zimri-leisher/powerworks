package screen.mouse.tool

import behavior.Behavior
import behavior.BehaviorTree
import graphics.Image
import graphics.Texture
import io.Control
import io.PressType
import level.LevelManager
import level.entity.Entity
import level.entity.EntityGroup
import misc.Geometry
import misc.PixelCoord
import network.MovingObjectReference
import player.ControlEntityAction
import player.PlayerManager
import screen.ScreenManager
import screen.elements.GUIMouseOverRegion
import screen.elements.GUITexturePane
import screen.elements.GUIWindow
import screen.mouse.Mouse

object ControllerMenu : GUIWindow("Entity controller window",
        0, 0, 55, 55, ScreenManager.Groups.HUD) {

    var background: GUITexturePane

    init {
        background = GUITexturePane(this, "Entity controller background", 0, 0, Image.GUI.ENTITY_CONTROLLER_MENU)
        background.apply {
            GUIMouseOverRegion(this, "move command mouse region", { 20 }, { 38 }, { 14 }, { 14 }, onEnter = {
                EntityController.currentlyHoveringCommand = Behavior.Movement.PATH_TO_FORMATION
                background.renderable = Texture(Image.GUI.ENTITY_CONTROLLER_MENU_MOVE_SELECTED)
            }, onLeave = {
                EntityController.currentlyHoveringCommand = null
            })
            GUIMouseOverRegion(this, "attack command mouse region", { 20 }, { 3 }, { 14 }, { 14 }, onEnter = {
                EntityController.currentlyHoveringCommand = Behavior.Offense.ATTACK_ARG
                background.renderable = Texture(Image.GUI.ENTITY_CONTROLLER_MENU_ATTACK_SELECTED)
            }, onLeave = {
                EntityController.currentlyHoveringCommand = null
            })
            GUIMouseOverRegion(this, "defend command mouse region", { 3 }, { 20 }, { 14 }, { 14 }, onEnter = {
                EntityController.currentlyHoveringCommand = null // DEF
                background.renderable = Texture(Image.GUI.ENTITY_CONTROLLER_MENU_DEFEND_SELECTED)
            }, onLeave = {
                EntityController.currentlyHoveringCommand = null
            })
            GUIMouseOverRegion(this, "stop command mouse region", { 37 }, { 20 }, { 14 }, { 14 }, onEnter = {
                EntityController.currentlyHoveringCommand = null // STP
                background.renderable = Texture(Image.GUI.ENTITY_CONTROLLER_MENU_STOP_SELECTED)
            }, onLeave = {
                EntityController.currentlyHoveringCommand = null
            })
        }
        transparentToInteraction = true
    }
}

object EntityController : Tool(Control.USE_ENTITY_COMMAND) {

    const val DRAG_DISTANCE_BEFORE_MENU_OPEN = 8

    var startXPixel = 0
    var startYPixel = 0

    var currentlyHoveringCommand: BehaviorTree? = null
    var selectedCommand: BehaviorTree? = null

    override fun onUse(control: Control, type: PressType, mouseLevelXPixel: Int, mouseLevelYPixel: Int): Boolean {
        if (control == Control.USE_ENTITY_COMMAND) {
            if (type == PressType.PRESSED) {
                startXPixel = Mouse.xPixel
                startYPixel = Mouse.yPixel
                println("start: $startXPixel, $startYPixel")
            } else if (type == PressType.REPEAT) {
                if (Geometry.distance(Mouse.xPixel, Mouse.yPixel, startXPixel, startYPixel) > DRAG_DISTANCE_BEFORE_MENU_OPEN) {
                    val startXCopy = startXPixel
                    val startYCopy = startYPixel // the unhappy (sometimes) magic of lambdas
                    ControllerMenu.alignments.x = { startXCopy - ControllerMenu.alignments.width() / 2 }
                    ControllerMenu.alignments.y = { startYCopy - ControllerMenu.alignments.height() / 2 }
                    ControllerMenu.open = true
                }
            } else {
                if (ControllerMenu.open) {
                    selectedCommand = currentlyHoveringCommand
                    ControllerMenu.open = false
                }
                if(!Selector.dragging) {
                    if (selectedCommand != null && !ControllerMenu.open) {
                        controlEntities(selectedCommand!!)
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun controlEntities(command: BehaviorTree) {
        when (command) {
            Behavior.Movement.PATH_TO_FORMATION -> {
                for (group in EntityGroup.ALL) {
                    if (group.formation != null) {
                        val boundaries = group.formation!!.boundaries
                        if (Geometry.contains(boundaries.x, boundaries.y, boundaries.width, boundaries.height,
                                        LevelManager.mouseLevelXPixel, LevelManager.mouseLevelYPixel, 0, 0)) {
                            // merge groups
                            // TODO
                        }
                    }
                }
                PlayerManager.takeAction(ControlEntityAction(PlayerManager.localPlayer,
                        Selector.currentSelected.filterIsInstance<Entity>().map { it.toReference() as MovingObjectReference },
                        command, PixelCoord(LevelManager.mouseLevelXPixel, LevelManager.mouseLevelYPixel)))
            }
            Behavior.Offense.ATTACK_ARG -> {
                PlayerManager.takeAction(ControlEntityAction(PlayerManager.localPlayer,
                        Selector.currentSelected.filterIsInstance<Entity>().map { it.toReference() as MovingObjectReference },
                        command, LevelManager.levelObjectUnderMouse?.toReference()))
            }
        }
    }
}