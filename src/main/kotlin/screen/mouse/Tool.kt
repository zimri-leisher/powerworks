package screen.mouse

import behavior.Behavior
import behavior.BehaviorTree
import com.badlogic.gdx.graphics.Color
import graphics.Image
import graphics.Renderer
import graphics.TextureRenderParams
import io.*
import item.BlockItemType
import item.ItemType
import item.EntityItemType
import level.Hitbox
import level.Level
import level.LevelObject
import level.block.Block
import level.block.BlockType
import level.entity.Entity
import level.moving.MovingObject
import main.Game
import main.toColor
import misc.Geometry
import misc.PixelCoord
import resource.ResourceNode
import screen.RoutingLanguageEditor
import screen.ScreenManager
import screen.elements.GUIMouseOverRegion
import screen.elements.GUIOutline
import screen.elements.GUITexturePane
import screen.elements.GUIWindow

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
            ResourceNodeEditor
            EntityPlacer
            EntityController
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

        object ResourceNodeEditor : Tool(Control.EDIT_RESOURCE_NODE) {

            var selectedNodes = listOf<ResourceNode>()

            override fun onUse(control: Control, type: PressType, mouseLevelXPixel: Int, mouseLevelYPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
                if (currentlyActive) {
                    if (type == PressType.RELEASED) {
                        RoutingLanguageEditor.node = selectedNodes.first()
                        RoutingLanguageEditor.open = true
                    }
                }
            }

            override fun renderAbove() {
            }

            override fun updateCurrentlyActive() {
                selectedNodes = Level.ResourceNodes.get(Game.currentLevel.mouseLevelXPixel shr 4, Game.currentLevel.mouseLevelYPixel shr 4)
                currentlyActive = selectedNodes.isNotEmpty()
            }
        }

        object EntityPlacer : Tool(Control.SPAWN_ENTITY) {

            var type: EntityItemType? = null
            var canSpawn = false

            override fun onUse(control: Control, type: PressType, mouseLevelXPixel: Int, mouseLevelYPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
                if (currentlyActive) {
                    if (type == PressType.RELEASED && control == Control.SPAWN_ENTITY) {
                        if (canSpawn) {
                            if (Level.add(this.type!!.spawnedEntity.instantiate(mouseLevelXPixel, mouseLevelYPixel, 0))) {
                                Game.mainInv.remove(this.type!!)
                            }
                        }
                    }
                }
            }

            override fun renderAbove() {
                val entity = type!!.spawnedEntity
                val texture = entity.textures[0]
                texture.render(Game.currentLevel.mouseLevelXPixel, Game.currentLevel.mouseLevelYPixel, params = TextureRenderParams(color = toColor(alpha = 0.4f)))
                Renderer.renderEmptyRectangle(Game.currentLevel.mouseLevelXPixel + entity.hitbox.xStart, Game.currentLevel.mouseLevelYPixel + entity.hitbox.yStart, entity.hitbox.width, entity.hitbox.height,
                        params = TextureRenderParams(color = toColor(
                                if (canSpawn) 0f else 1f, if (canSpawn) 1f else 0f, 0f, 0.4f)))
            }

            override fun update() {
                canSpawn = Game.currentLevel.selectedLevelObject == null && Level.getCollision(type!!.spawnedEntity, Game.currentLevel.mouseLevelXPixel, Game.currentLevel.mouseLevelYPixel) == null
            }

            override fun updateCurrentlyActive() {
                val item = Mouse.heldItemType
                currentlyActive = Game.currentLevel.selectedLevelObject == null && item != null && item is EntityItemType && !Selector.dragging
                if (currentlyActive) {
                    type = item as EntityItemType
                }
            }
        }

        object EntityController : Tool(Control.SELECT_ENTITY_COMMAND, Control.USE_ENTITY_COMMAND) {

            const val DRAG_DISTANCE_BEFORE_MENU_OPEN = 8

            var startXPixel = 0
            var startYPixel = 0

            var currentlyHoveringCommand: BehaviorTree? = null
            var selectedCommand: BehaviorTree? = null

            object ControllerMenu : GUIWindow("Entity controller window",
                    0, 0, 55, 55, ScreenManager.Groups.HUD) {
                init {
                    GUITexturePane(this, "Entity controller background", 0, 0, Image.GUI.ENTITY_CONTROLLER_MENU).run {
                        GUIMouseOverRegion(this, "move command mouse region", { 20 }, { 38 }, { 14 }, { 14 }, onEnter = {
                            currentlyHoveringCommand = Behavior.Movement.TO_MOUSE
                        }, onLeave = {
                            currentlyHoveringCommand = null
                        })
                        GUIMouseOverRegion(this, "attack command mouse region", { 20 }, { 3 }, { 14 }, { 14 }, onEnter = {
                            currentlyHoveringCommand = Behavior.Offense.ATTACK_ARGUMENT // ATK
                        }, onLeave = {
                            currentlyHoveringCommand = null
                        })
                        GUIMouseOverRegion(this, "defend command mouse region", { 3 }, { 20 }, { 14 }, { 14 }, onEnter = {
                            currentlyHoveringCommand = null // DEF
                        }, onLeave = {
                            currentlyHoveringCommand = null
                        })
                        GUIMouseOverRegion(this, "stop command mouse region", { 37 }, { 20 }, { 14 }, { 14 }, onEnter = {
                            currentlyHoveringCommand = null // STP
                        }, onLeave = {
                            currentlyHoveringCommand = null
                        })
                    }
                    transparentToInteraction = true
                }
            }

            override fun onUse(control: Control, type: PressType, mouseLevelXPixel: Int, mouseLevelYPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
                if (currentlyActive) {
                    if (control == Control.SELECT_ENTITY_COMMAND) {
                        if (type == PressType.PRESSED) {
                            startXPixel = Mouse.xPixel
                            startYPixel = Mouse.yPixel
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
                                /*
                                if (selectedCommand == // STOP) {
                                    selectedCommand!!.execute(Selector.selected.filterIsInstance<Entity>())
                                    selectedCommand = null
                                }
                                 */
                                ControllerMenu.open = false
                            }
                        }
                    } else if (control == Control.USE_ENTITY_COMMAND) {
                        if (type == PressType.PRESSED) {
                            if (selectedCommand != null && !ControllerMenu.open) {
                                Selector.selected.filterIsInstance<Entity>().forEach { it.runBehavior(selectedCommand!!) }
                            }
                        }
                    }
                }
            }

            override fun updateCurrentlyActive() {
                currentlyActive = true
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
                            if (Selector.selected.isNotEmpty()) {
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

            var type: BlockItemType? = null
            var xTile = 0
            var yTile = 0
            var rotation = 0
            var canPlace = false

            override fun onUse(control: Control, type: PressType, mouseLevelXPixel: Int, mouseLevelYPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
                if (currentlyActive) {
                    if (type == PressType.PRESSED) {
                        if (control == Control.ROTATE_BLOCK) {
                            rotation = (rotation + 1) % 4
                        }
                    }
                    if (type == PressType.RELEASED) {
                        if (control == Control.PLACE_BLOCK) {
                            if (canPlace) {
                                if (Level.add(this.type!!.placedBlock.instantiate(xTile shl 4, yTile shl 4, rotation))) {
                                    Game.mainInv.remove(this.type!!)
                                }
                            }
                        }
                    }
                }
            }

            override fun updateCurrentlyActive() {
                val item = Mouse.heldItemType
                currentlyActive = Game.currentLevel.selectedLevelObject == null && item is BlockItemType && item.placedBlock != BlockType.ERROR && !Selector.dragging
                if (currentlyActive) {
                    type = item as BlockItemType
                } else {
                    type = null
                }
            }

            override fun update() {
                xTile = ((Game.currentLevel.mouseLevelXPixel) shr 4) - type!!.placedBlock.widthTiles / 2
                yTile = ((Game.currentLevel.mouseLevelYPixel) shr 4) - type!!.placedBlock.heightTiles / 2
                canPlace = Level.getCollision(type!!.placedBlock, xTile shl 4, yTile shl 4) == null
            }

            override fun renderAbove() {
                val blockType = type!!.placedBlock
                val xPixel = xTile shl 4
                val yPixel = yTile shl 4
                blockType.textures.render(xPixel, yPixel, rotation, TextureRenderParams(color = Color(1f, 1f, 1f, 0.4f)))
                Renderer.renderEmptyRectangle(xPixel, yPixel, blockType.widthTiles shl 4, blockType.heightTiles shl 4, params = TextureRenderParams(color = toColor(if (canPlace) 0x04C900 else 0xC90004, 0.3f)))
                Renderer.renderTextureKeepAspect(Image.Misc.ARROW, xPixel, yPixel, blockType.widthTiles shl 4, blockType.heightTiles shl 4, TextureRenderParams(rotation = 90f * rotation))
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

            var selected = mutableSetOf<LevelObject>()

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
                    selected = mutableSetOf()
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
                        SelectorMode.MOVING_ONLY -> {
                            Level.MovingObjects.getIntersectingFromPixelRectangle(x, y, w, h).toMutableSet()
                        }
                        SelectorMode.ALL -> {
                            (Level.Blocks.getIntersectingFromPixelRectangle(x, y, w, h).toList() +
                                    Level.MovingObjects.getIntersectingFromPixelRectangle(x, y, w, h)).toMutableSet()
                        }
                        SelectorMode.BLOCKS_ONLY -> {
                            Level.Blocks.getIntersectingFromPixelRectangle(x, y, w, h).toMutableSet()
                        }
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
                if (type == PressType.PRESSED) {
                    val obj = Game.currentLevel.selectedLevelObject
                    if (obj != null) {
                        //println(JSON.unquoted.stringify(obj.serialize(), obj))
                    }

                }
            }

            override fun updateCurrentlyActive() {
                currentlyActive = true
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