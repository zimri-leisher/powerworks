package screen.mouse

import com.badlogic.gdx.graphics.Color
import graphics.Renderer
import graphics.TextureRenderParams
import io.*
import item.BlockItemType
import item.ItemType
import level.Hitbox
import level.Level
import level.LevelObject
import level.block.Block
import level.block.BlockType
import level.block.GhostBlock
import level.moving.MovingObject
import main.Game
import main.toColor

/**
 * A tool is an object that is used by the client to interact with the level through the mouse
 * @param use the controls to send to this tool when they are pressed/repeated/released. These should be what will 'use' the tool
 */
abstract class Tool(vararg val use: Control) : ControlPressHandler {

    constructor(use: Control.Group) : this(*use.controls)

    /**
     * Whether or not this tool is currently able to be used
     */
    var currentlyActive = false
        set(value) {
            if (value && !field) {
                ALL_ACTIVE.add(this)
                field = value
            } else if (!value && field) {
                ALL_ACTIVE.remove(this)
                field = value
            }
        }

    init {
        ALL.add(this)
        InputManager.registerControlPressHandler(this, ControlPressHandlerType.LEVEL_ANY, *use)
    }

    override fun handleControlPress(p: ControlPress) {
        onUse(p.control, p.pressType, Game.currentLevel.mouseLevelXPixel, Game.currentLevel.mouseLevelYPixel, Mouse.button, InputManager.inputsBeingPressed.contains("SHIFT"), InputManager.inputsBeingPressed.contains("CONTROL"), InputManager.inputsBeingPressed.contains("ALT"))
    }

    /**
     * This is called when the control matching the 'use' variable is pressed, even if this tool is not currently active.
     * Thus, you have to check if currentlyActive is true before actually executing the tool here.
     */
    abstract fun onUse(control: Control, type: PressType, mouseLevelXPixel: Int, mouseLevelYPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean)

    /**
     * Updates the currentlyActive variable to the correct value (true if this tool should be able to be used, false otherwise)
     * For example, if the INTERACTOR tool were over a level object with isInteractable set to true, this should set currentlyActive to true
     */
    abstract fun updateCurrentlyActive()

    /**
     * Whatever is drawn here is drawn above all level objects
     * This is only called if the tool is active
     */
    open fun renderAbove() {}

    /**
     * Whatever is drawn here is drawn below all level objects
     * This is only called if the tool is active
     */
    open fun renderBelow() {}

    /**
     * This is only called if the tool is active
     */
    open fun update() {}

    companion object {

        val ALL = mutableListOf<Tool>()

        val ALL_ACTIVE = mutableListOf<Tool>()

        init {
            Interactor
            BlockRemover
            BlockPlacer
            Teleporter
            Selector
            Debug
        }

        object Interactor : Tool(Control.Group.INTERACTION) {
            override fun onUse(control: Control, type: PressType, mouseLevelXPixel: Int, mouseLevelYPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
                if (currentlyActive) {
                    if (control in Control.Group.SCROLL)
                        Game.currentLevel.selectedLevelObject!!.onScroll(if (control == Control.SCROLL_DOWN) -1 else 1)
                    else
                        Game.currentLevel.selectedLevelObject!!.onInteractOn(type, mouseLevelXPixel, mouseLevelYPixel, button, shift, ctrl, alt)
                }
            }

            override fun updateCurrentlyActive() {
                currentlyActive = Game.currentLevel.selectedLevelObject?.isInteractable == true
            }

            override fun renderBelow() {
                val s = Game.currentLevel.selectedLevelObject!!
                if (s is Block)
                    Renderer.renderEmptyRectangle(s.xPixel - 1, s.yPixel - 1, (s.type.widthTiles shl 4) + 2, (s.type.heightTiles shl 4) + 2, params = TextureRenderParams(color = Color(0x1A6AF472)))
                else if (s is MovingObject)
                    Renderer.renderEmptyRectangle(s.xPixel + s.hitbox.xStart - 1, s.yPixel + s.hitbox.yStart - 1, s.hitbox.width + 2, s.hitbox.height + 2, params = TextureRenderParams(color = Color(0x1A6AF472)))
            }
        }

        object BlockRemover : Tool(Control.REMOVE_SELECTED_BLOCKS) {
            override fun updateCurrentlyActive() {
                currentlyActive = Game.currentLevel.selectedLevelObject != null
            }

            override fun onUse(control: Control, type: PressType, mouseLevelXPixel: Int, mouseLevelYPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
                if (currentlyActive) {
                    if (type == PressType.PRESSED) {
                        if (control == Control.REMOVE_SELECTED_BLOCKS) {
                            if(Selector.selected.isNotEmpty()) {
                                Selector.selected.filter { it is Block }.forEach {
                                    it as Block
                                    Level.remove(it)
                                    Selector.selected.remove(it)
                                    val item = ItemType.ALL.firstOrNull { item -> item is BlockItemType && item.placedBlock == it.type }
                                    if (item != null) {
                                        Game.mainInv.add(item)
                                    }
                                }
                            } else {
                                val o = Game.currentLevel.selectedLevelObject!!
                                if (o is Block) {
                                    Level.remove(o)
                                    val item = ItemType.ALL.firstOrNull { it is BlockItemType && it.placedBlock == o.type }
                                    if (item != null) {
                                        Game.mainInv.add(item)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        object BlockPlacer : Tool(Control.PLACE_BLOCK, Control.ROTATE_BLOCK), ControlPressHandler {

            var ghostBlock: GhostBlock? = null
            var ghostBlockRotation = 0

            override fun onUse(control: Control, type: PressType, mouseLevelXPixel: Int, mouseLevelYPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
                if (currentlyActive) {
                    if (type == PressType.PRESSED) {
                        if (control == Control.ROTATE_BLOCK) {
                            ghostBlockRotation = (ghostBlockRotation + 1) % 4
                        }
                    }
                    if (type == PressType.RELEASED) {
                        if (control == Control.PLACE_BLOCK) {
                            val block = Game.currentLevel.selectedLevelObject
                            if (block == null) {
                                if (ghostBlock != null) {
                                    val gBlock = ghostBlock!!
                                    if (Level.add(gBlock.type.instantiate(gBlock.xPixel, gBlock.yPixel, gBlock.rotation))) {
                                        val h = Mouse.heldItemType!! // todo why is this crashing occasionally
                                        Game.mainInv.remove(h, 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            override fun updateCurrentlyActive() {
                currentlyActive = Game.currentLevel.selectedLevelObject == null && Mouse.heldItemType is BlockItemType && !Selector.dragging
            }

            override fun update() {
                val currentItem = Mouse.heldItemType
                if (currentItem is BlockItemType) {
                    if (currentItem.placedBlock == BlockType.ERROR || Game.mainInv.getQuantity(currentItem) == 0) {
                        ghostBlock = null
                    } else if (currentItem.placedBlock != BlockType.ERROR) {
                        val placedType = currentItem.placedBlock
                        val xTile = ((Game.currentLevel.mouseLevelXPixel) shr 4) - placedType.widthTiles / 2
                        val yTile = ((Game.currentLevel.mouseLevelYPixel) shr 4) - placedType.heightTiles / 2
                        ghostBlock = GhostBlock(placedType, xTile, yTile, ghostBlockRotation)
                    }
                } else {
                    ghostBlock = null
                }
            }

            override fun renderAbove() {
                if (ghostBlock != null) {
                    val g = ghostBlock!!
                    g.render()
                }
            }
        }

        object Teleporter : Tool(Control.TELEPORT) {

            override fun onUse(control: Control, type: PressType, mouseLevelXPixel: Int, mouseLevelYPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
                if (currentlyActive) {
                    if (type == PressType.PRESSED) {
                        (Game.currentLevel.viewBeingInteractedWith?.camera as? MovingObject)?.setPosition(Game.currentLevel.mouseLevelXPixel, Game.currentLevel.mouseLevelYPixel)
                    }
                }
            }

            override fun updateCurrentlyActive() {
                currentlyActive = true
            }

        }

        private enum class SelectorMode {
            ALL, BLOCKS_ONLY, MOVING_ONLY
        }

        const val SELECTION_START_THRESHOLD = 2

        object Selector : Tool(Control.Group.SELECTOR_TOOLS) {

            var dragging = false
            var dragStartXPixel = 0
            var dragStartYPixel = 0
            var currentDragXPixel = 0
            var currentDragYPixel = 0

            private var mode = SelectorMode.ALL

            var selected = mutableListOf<LevelObject>()

            override fun update() {
                selected.removeIf { !it.inLevel }
            }

            override fun updateCurrentlyActive() {
                currentlyActive = dragging || selected.isNotEmpty()
            }

            override fun onUse(control: Control, type: PressType, mouseLevelXPixel: Int, mouseLevelYPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
                when (control) {
                    Control.START_SELECTION_ALL_OBJECTS -> mode = SelectorMode.ALL
                    Control.START_SELECTION_BLOCKS_ONLY -> mode = SelectorMode.BLOCKS_ONLY
                    Control.START_SELECTION_MOVING_ONLY -> mode = SelectorMode.MOVING_ONLY
                }
                if (type == PressType.PRESSED) {
                    selected = mutableListOf()
                    dragStartXPixel = mouseLevelXPixel
                    dragStartYPixel = mouseLevelYPixel
                    currentDragXPixel = mouseLevelXPixel
                    currentDragYPixel = mouseLevelYPixel
                } else if (type == PressType.REPEAT) {
                    currentDragXPixel = mouseLevelXPixel
                    currentDragYPixel = mouseLevelYPixel
                    if (Math.abs(dragStartXPixel - currentDragXPixel) > SELECTION_START_THRESHOLD || Math.abs(dragStartYPixel - currentDragYPixel) > SELECTION_START_THRESHOLD) {
                        dragging = true
                        updateSelected()
                    }
                } else if (type == PressType.RELEASED) {
                    if (dragging) {
                        updateSelected()
                        dragging = false
                    }
                }
            }

            private fun updateSelected() {
                if (dragging) {
                    val xChange = currentDragXPixel - dragStartXPixel
                    val yChange = currentDragYPixel - dragStartYPixel
                    val x = if (xChange < 0) currentDragXPixel else dragStartXPixel
                    val y = if (yChange < 0) currentDragYPixel else dragStartYPixel
                    val w = Math.abs(xChange)
                    val h = Math.abs(yChange)
                    selected = when (mode) {
                        SelectorMode.ALL ->
                            (Level.Blocks.getIntersectingFromPixelRectangle(x, y, w, h).toList() +
                                    Level.MovingObjects.getFromPixelRectangle(x, y, w, h)).toMutableList()
                        SelectorMode.BLOCKS_ONLY ->
                            Level.Blocks.getIntersectingFromPixelRectangle(x, y, w, h).toMutableList()
                        SelectorMode.MOVING_ONLY ->
                            Level.MovingObjects.getFromPixelRectangle(x, y, w, h).toMutableList()
                    }
                }
            }

            override fun renderBelow() {
                if (dragging) {
                    Renderer.renderEmptyRectangle(dragStartXPixel, dragStartYPixel, (currentDragXPixel - dragStartXPixel), (currentDragYPixel - dragStartYPixel), params = TextureRenderParams(color = toColor(0.8f, 0.8f, 0.8f)))
                }
                for (l in selected) {
                    if (l is Block) {
                        Renderer.renderEmptyRectangle(l.xPixel - 1, l.yPixel - 1, (l.type.widthTiles shl 4) + 2, (l.type.heightTiles shl 4) + 2, params = TextureRenderParams(color = toColor(alpha = 0.2f)))
                    } else if (l is MovingObject && l.hitbox != Hitbox.NONE) {
                        Renderer.renderEmptyRectangle(l.xPixel + l.hitbox.xStart - 1, l.yPixel + l.hitbox.yStart - 1, l.hitbox.width + 2, l.hitbox.height + 2, params = TextureRenderParams(color = toColor(alpha = 0.2f)))
                    }
                }
            }
        }

        object Debug : Tool(Control.Group.INTERACTION) {

            override fun onUse(control: Control, type: PressType, mouseLevelXPixel: Int, mouseLevelYPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
                if (currentlyActive) {
                    if (type == PressType.PRESSED) {
                        Level.Tiles.get(mouseLevelXPixel shr 4, mouseLevelYPixel shr 4).apply {
                            rotation = (rotation + 1) % 4
                        }
                    }
                }
            }

            override fun updateCurrentlyActive() {
                currentlyActive = false
            }

            override fun renderAbove() {
                Renderer.renderText(Level.Tiles.get(Game.currentLevel.mouseLevelXPixel shr 4, Game.currentLevel.mouseLevelYPixel shr 4).rotation, ((Game.currentLevel.mouseLevelXPixel shr 4) shl 4) + 4, ((Game.currentLevel.mouseLevelYPixel shr 4) shl 4) + 4)
            }

        }

        fun renderBelow() {
            ALL_ACTIVE.forEach { it.renderBelow() }
        }

        fun renderAbove() {
            ALL_ACTIVE.forEach { it.renderAbove() }
        }

        fun update() {
            ALL.forEach { it.updateCurrentlyActive() }
            ALL_ACTIVE.forEach { it.update() }
        }
    }
}