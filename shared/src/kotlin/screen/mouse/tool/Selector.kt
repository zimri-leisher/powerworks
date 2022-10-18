package screen.mouse.tool

import graphics.Renderer
import graphics.TextureRenderParams
import io.*
import level.*
import level.block.Block
import level.entity.Entity
import level.moving.MovingObject
import main.toColor
import network.MovingObjectReference
import player.ActionEntityCreateGroup
import player.PlayerManager
import screen.mouse.Mouse
import kotlin.math.abs

private enum class SelectorMode {
    ALL, ADD, SUBTRACT
}

object Selector : Tool(Control.Group.SELECTOR_TOOLS.controls), ControlEventHandler {

    private const val SELECTION_START_THRESHOLD = 2

    var startPress = false
    var selectingInLevel: Level = LevelManager.EMPTY_LEVEL
    var dragging = false
    var dragStartX = 0
    var dragStartY = 0
    var currentDragX = 0
    var currentDragY = 0

    private var mode = SelectorMode.ALL

    var currentSelected = mutableSetOf<PhysicalLevelObject>()
    var newlySelected = mutableSetOf<PhysicalLevelObject>()

    init {
        InputManager.register(this, Control.Group.SELECTOR_TOOLS)
        activationPredicate = {
            Mouse.heldItemType == null
        }
    }

    override fun update() {
        currentSelected.removeIf { it.level != selectingInLevel || !it.inLevel }
        newlySelected.removeIf { it.level != selectingInLevel || !it.inLevel}
        if (startPress && LevelManager.levelUnderMouse != null) {
            currentDragX = LevelManager.mouseLevelX
            currentDragY = LevelManager.mouseLevelY
            if (Math.abs(dragStartX - currentDragX) > SELECTION_START_THRESHOLD || Math.abs(dragStartY - currentDragY) > SELECTION_START_THRESHOLD) {
                dragging = true
                selectingInLevel = LevelManager.levelUnderMouse!!
                updateSelected()
            }
        }
    }

    override fun onUse(event: ControlEvent, mouseLevelX: Int, mouseLevelY: Int): Boolean {
        if (LevelManager.levelUnderMouse == null) {
            return false
        }
        when (event.control) {
            Control.START_SELECTION -> mode = SelectorMode.ALL
            Control.START_SELECTION_ADD -> mode = SelectorMode.ADD
            Control.START_SELECTION_SUBTRACT -> mode = SelectorMode.SUBTRACT
            else -> {}
        }
        if (event.type == ControlEventType.PRESS) {
            startPress = true
            if (mode == SelectorMode.ALL) {
                currentSelected = mutableSetOf()
            }
            newlySelected = mutableSetOf()
            dragStartX = mouseLevelX
            dragStartY = mouseLevelY
            currentDragX = mouseLevelX
            currentDragY = mouseLevelY
            return false
        } else if (event.type == ControlEventType.HOLD) {
            return true
        } else if (event.type == ControlEventType.RELEASE) {
            if (dragging) {
                updateSelected()
                currentSelected = when (mode) {
                    SelectorMode.ADD -> currentSelected + newlySelected
                    SelectorMode.ALL -> newlySelected
                    SelectorMode.SUBTRACT -> currentSelected - newlySelected
                }.toMutableSet()
                val entitiesSelected = currentSelected.filterIsInstance<Entity>()
                if (entitiesSelected.isNotEmpty()) {
                    PlayerManager.takeAction(ActionEntityCreateGroup(PlayerManager.localPlayer, entitiesSelected))
                }
                newlySelected = mutableSetOf()
                dragging = false
            }
            startPress = false
            return true
        }
        return false
    }

    private fun updateSelected() {
        if (dragging && LevelManager.levelUnderMouse != null) {
            val xChange = currentDragX - dragStartX
            val yChange = currentDragY - dragStartY
            val x = if (xChange < 0) currentDragX else dragStartX
            val y = if (yChange < 0) currentDragY else dragStartY
            val w = abs(xChange)
            val h = abs(yChange)
            newlySelected =
                    (LevelManager.levelUnderMouse!!.getIntersectingBlocksFromRectangle(x, y, w, h).toMutableSet() +
                            LevelManager.levelUnderMouse!!.getMovingObjectCollisions(x, y, w, h)).toMutableSet()
        }
    }

    override fun renderBelow(level: Level) {
        if (dragging && level == selectingInLevel) {
            Renderer.renderEmptyRectangle(dragStartX, dragStartY, (currentDragX - dragStartX), (currentDragY - dragStartY), params = TextureRenderParams(color = toColor(0.8f, 0.8f, 0.8f)))
        }
        val selection = if (dragging) when (mode) {
            SelectorMode.ADD -> currentSelected + newlySelected
            SelectorMode.ALL -> newlySelected
            SelectorMode.SUBTRACT -> currentSelected - newlySelected
        } else currentSelected
        for (l in selection) {
            if (l.level == level) {
                if (l is Block) {
                    Renderer.renderEmptyRectangle(l.x - 1, l.y - 1, (l.type.widthTiles shl 4) + 2, (l.type.heightTiles shl 4) + 2, params = TextureRenderParams(color = toColor(alpha = 0.2f)))
                } else if (l is MovingObject && l.hitbox != Hitbox.NONE) {
                    Renderer.renderEmptyRectangle(l.x + l.hitbox.xStart - 1, l.y + l.hitbox.yStart - 1, l.hitbox.width + 2, l.hitbox.height + 2, params = TextureRenderParams(color = toColor(alpha = 0.2f)))
                }
            }
        }
    }

    override fun handleControlEvent(event: ControlEvent) {
        if(event.type == ControlEventType.RELEASE) {
            if (dragging) {
                updateSelected()
                currentSelected = when (mode) {
                    SelectorMode.ADD -> currentSelected + newlySelected
                    SelectorMode.ALL -> newlySelected
                    SelectorMode.SUBTRACT -> currentSelected - newlySelected
                }.toMutableSet()
                val entitiesSelected = currentSelected.filterIsInstance<Entity>()
                if (entitiesSelected.isNotEmpty()) {
                    PlayerManager.takeAction(ActionEntityCreateGroup(PlayerManager.localPlayer, entitiesSelected))
                }
                newlySelected = mutableSetOf()
                dragging = false
            }
            startPress = false
        }
    }
}