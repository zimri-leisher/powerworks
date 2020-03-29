package screen.mouse

import behavior.Behavior
import behavior.BehaviorTree
import com.badlogic.gdx.graphics.Color
import graphics.Image
import graphics.Renderer
import graphics.Texture
import graphics.TextureRenderParams
import io.*
import item.BlockItemType
import item.EntityItemType
import item.Inventory
import level.*
import level.block.Block
import level.block.BlockType
import level.entity.Entity
import level.moving.MovingObject
import main.Game
import main.toColor
import misc.Geometry
import network.ClientNetworkManager
import network.packet.*
import player.PlayerManager
import resource.ResourceCategory
import resource.ResourceNode
import routing.RoutingLanguage
import screen.RoutingLanguageEditor
import screen.ScreenManager
import screen.elements.GUIMouseOverRegion
import screen.elements.GUITexturePane
import screen.elements.GUIWindow

/**
 * A tool is an object that is used by the player to interact with the [Level], usually through the mouse
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
        if(!Game.IS_SERVER) {
            InputManager.registerControlPressHandler(this, ControlPressHandlerType.LEVEL_ANY_UNDER_MOUSE, *use)
        }
    }

    override fun handleControlPress(p: ControlPress) {
        if (currentlyActive) {
            onUse(p.control, p.pressType, LevelManager.mouseLevelXPixel, LevelManager.mouseLevelYPixel, Mouse.button, InputManager.inputsBeingPressed.contains("SHIFT"), InputManager.inputsBeingPressed.contains("CONTROL"), InputManager.inputsBeingPressed.contains("ALT"))
        }
    }

    /**
     * This is called when a control from [Tool.use] is pressed/repeated/released, and [currentlyActive] is true. Tools are
     * unable to be used if the mouse isn't over a [Level], so [LevelManager.levelUnderMouse] can be assumed to be non-null
     */
    abstract fun onUse(control: Control, type: PressType, mouseLevelXPixel: Int, mouseLevelYPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean)

    /**
     * Updates the [currentlyActive] variable to the correct value (true if this tool should be able to be used, false otherwise)
     * For example, if the [Tool.Interactor] tool were over a level object with [LevelObject.isInteractable] set to true, this should set currentlyActive to true
     */
    abstract fun updateCurrentlyActive()

    /**
     * Whatever is drawn here is drawn above all level objects.
     * This is only called if [currentlyActive] is true
     */
    open fun renderAbove() {}

    /**
     * Whatever is drawn here is drawn below all level objects.
     * This is only called if [currentlyActive] is true
     */
    open fun renderBelow() {}

    /**
     * An updater called once every game update, but only if [currentlyActive] is true
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
            // todo priority: if onUse returns true and this is the highest priority, none of the other controls will go through
        }

        object Interactor : Tool(Control.Group.INTERACTION) {
            override fun onUse(control: Control, type: PressType, mouseLevelXPixel: Int, mouseLevelYPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
                if (control in Control.Group.SCROLL)
                    LevelManager.levelObjectUnderMouse!!.onScroll(if (control == Control.SCROLL_DOWN) -1 else 1)
                else {
                    LevelManager.levelObjectUnderMouse!!.onInteractOn(type, mouseLevelXPixel, mouseLevelYPixel, button, shift, ctrl, alt)
                    LevelManager.onInteractWithLevelObject()
                }
            }

            override fun updateCurrentlyActive() {
                currentlyActive = LevelManager.levelObjectUnderMouse?.isInteractable == true
            }

            override fun renderBelow() {
                val s = LevelManager.levelObjectUnderMouse
                if (s is Block)
                    Renderer.renderEmptyRectangle(s.xPixel - 1, s.yPixel - 1, (s.type.widthTiles shl 4) + 2, (s.type.heightTiles shl 4) + 2, params = TextureRenderParams(color = Color(0x1A6AF472)))
                else if (s is MovingObject)
                    Renderer.renderEmptyRectangle(s.xPixel + s.hitbox.xStart - 1, s.yPixel + s.hitbox.yStart - 1, s.hitbox.width + 2, s.hitbox.height + 2, params = TextureRenderParams(color = Color(0x1A6AF472)))
            }
        }

        object ResourceNodeEditor : Tool(Control.EDIT_RESOURCE_NODE) {

            var selectedNodes = setOf<ResourceNode>()

            override fun onUse(control: Control, type: PressType, mouseLevelXPixel: Int, mouseLevelYPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
                if (type == PressType.RELEASED) {
                    RoutingLanguageEditor.node = selectedNodes.first()
                    RoutingLanguageEditor.open = true
                }
            }

            override fun updateCurrentlyActive() {
                selectedNodes = LevelManager.levelUnderMouse?.getResourceNodesAt(LevelManager.mouseLevelXPixel shr 4, LevelManager.mouseLevelYPixel shr 4)
                        ?: emptySet()
                currentlyActive = selectedNodes.isNotEmpty()
            }
        }

        object EntityPlacer : Tool(Control.SPAWN_ENTITY) {

            var type: EntityItemType? = null
            var canSpawn = false

            override fun onUse(control: Control, type: PressType, mouseLevelXPixel: Int, mouseLevelYPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
                if (type == PressType.RELEASED && control == Control.SPAWN_ENTITY) {
                    if (canSpawn) {
                        if (LevelManager.levelUnderMouse!!.add(this.type!!.spawnedEntity.instantiate(mouseLevelXPixel, mouseLevelYPixel, 0))) {
                            PlayerManager.localPlayer.brainRobot.inventory.remove(this.type!!)
                        }
                    }
                }
            }

            override fun renderAbove() {
                val entity = type!!.spawnedEntity
                val texture = entity.textures[0]
                texture.render(LevelManager.mouseLevelXPixel, LevelManager.mouseLevelYPixel, params = TextureRenderParams(color = toColor(alpha = 0.4f)))
                Renderer.renderEmptyRectangle(LevelManager.mouseLevelXPixel + entity.hitbox.xStart, LevelManager.mouseLevelYPixel + entity.hitbox.yStart, entity.hitbox.width, entity.hitbox.height,
                        params = TextureRenderParams(color = toColor(
                                if (canSpawn) 0f else 1f, if (canSpawn) 1f else 0f, 0f, 0.4f)))
            }

            override fun update() {
                canSpawn = LevelManager.levelObjectUnderMouse == null && LevelManager.levelUnderMouse?.getCollisionsWith(type!!.spawnedEntity.hitbox, LevelManager.mouseLevelXPixel, LevelManager.mouseLevelYPixel)?.isEmpty() ?: false
            }

            override fun updateCurrentlyActive() {
                val item = Mouse.heldItemType
                currentlyActive = LevelManager.levelObjectUnderMouse == null && item != null && item is EntityItemType && !Selector.dragging
                if (currentlyActive) {
                    type = item as EntityItemType
                } else {
                    type = null
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

                var background: GUITexturePane

                init {
                    background = GUITexturePane(this, "Entity controller background", 0, 0, Image.GUI.ENTITY_CONTROLLER_MENU)
                    background.apply {
                        GUIMouseOverRegion(this, "move command mouse region", { 20 }, { 38 }, { 14 }, { 14 }, onEnter = {
                            currentlyHoveringCommand = Behavior.Movement.PATH_TO_MOUSE
                            background.renderable = Texture(Image.GUI.ENTITY_CONTROLLER_MENU_MOVE_SELECTED)
                        }, onLeave = {
                            currentlyHoveringCommand = null
                        })
                        GUIMouseOverRegion(this, "attack command mouse region", { 20 }, { 3 }, { 14 }, { 14 }, onEnter = {
                            currentlyHoveringCommand = null // ATK
                            background.renderable = Texture(Image.GUI.ENTITY_CONTROLLER_MENU_ATTACK_SELECTED)
                        }, onLeave = {
                            currentlyHoveringCommand = null
                        })
                        GUIMouseOverRegion(this, "defend command mouse region", { 3 }, { 20 }, { 14 }, { 14 }, onEnter = {
                            currentlyHoveringCommand = null // DEF
                            background.renderable = Texture(Image.GUI.ENTITY_CONTROLLER_MENU_DEFEND_SELECTED)
                        }, onLeave = {
                            currentlyHoveringCommand = null
                        })
                        GUIMouseOverRegion(this, "stop command mouse region", { 37 }, { 20 }, { 14 }, { 14 }, onEnter = {
                            currentlyHoveringCommand = null // STP
                            background.renderable = Texture(Image.GUI.ENTITY_CONTROLLER_MENU_STOP_SELECTED)
                        }, onLeave = {
                            currentlyHoveringCommand = null
                        })
                    }
                    transparentToInteraction = true
                }
            }

            override fun onUse(control: Control, type: PressType, mouseLevelXPixel: Int, mouseLevelYPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
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
                            if (selectedCommand == STOP) {
                                selectedCommand!!.execute(Selector.selected.filterIsInstance<Entity>())
                                selectedCommand = null
                            }
                             */
                            ControllerMenu.open = false
                        }
                    }
                } else if (control == Control.USE_ENTITY_COMMAND) {
                    if (type == PressType.RELEASED && !Selector.dragging) {
                        if (selectedCommand != null && !ControllerMenu.open) {
                            Selector.selected.filterIsInstance<Entity>().forEach { it.runBehavior(selectedCommand!!) }
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
                currentlyActive = LevelManager.levelObjectUnderMouse != null
            }

            override fun onUse(control: Control, type: PressType, mouseLevelXPixel: Int, mouseLevelYPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
                if (type == PressType.PRESSED) {
                    if (control == Control.REMOVE_SELECTED_BLOCKS) {
                        if (Selector.selected.isNotEmpty()) {
                            Selector.selected.filterIsInstance<Block>().forEach {
                                it.level.remove(it)
                            }
                        } else {
                            val o = LevelManager.levelObjectUnderMouse!!
                            if (o is Block) {
                                o.level.remove(o)
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
                if (type == PressType.PRESSED) {
                    if (control == Control.ROTATE_BLOCK) {
                        rotation = (rotation + 1) % 4
                    }
                }
                if (type == PressType.RELEASED) {
                    if (control == Control.PLACE_BLOCK) {
                        if (canPlace) {
                            val blockType = this.type!!.placedBlock
                            val block = blockType.instantiate(xTile shl 4, yTile shl 4, rotation)
                            LevelManager.levelUnderMouse?.add(block)
                        }
                    }
                }
            }

            override fun updateCurrentlyActive() {
                val item = Mouse.heldItemType
                currentlyActive = LevelManager.levelObjectUnderMouse == null && item is BlockItemType && item.placedBlock != BlockType.ERROR && !Selector.dragging
                if (currentlyActive) {
                    type = item as BlockItemType
                } else {
                    type = null
                }
            }

            override fun update() {
                xTile = ((LevelManager.mouseLevelXPixel) shr 4) - type!!.placedBlock.widthTiles / 2
                yTile = ((LevelManager.mouseLevelYPixel) shr 4) - type!!.placedBlock.heightTiles / 2
                canPlace = LevelManager.levelUnderMouse!!.canAdd(type!!.placedBlock, xTile shl 4, yTile shl 4)
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
                if (type == PressType.PRESSED) {
                    (LevelManager.levelViewUnderMouse?.camera as? MovingObject)?.setPosition(LevelManager.mouseLevelXPixel, LevelManager.mouseLevelYPixel)
                }
            }

            override fun updateCurrentlyActive() {
                currentlyActive = true
            }

        }

        private enum class SelectorMode {
            ALL, BLOCKS_ONLY, MOVING_ONLY
        }

        object Selector : Tool(Control.Group.SELECTOR_TOOLS), PacketHandler {

            private const val SELECTION_START_THRESHOLD = 2

            var dragging = false
            var dragStartXPixel = 0
            var dragStartYPixel = 0
            var currentDragXPixel = 0
            var currentDragYPixel = 0

            private var mode = SelectorMode.ALL

            var selected = mutableSetOf<LevelObject>()

            init {
                ClientNetworkManager.registerServerPacketHandler(this, PacketType.REMOVE_BLOCK)
            }

            override fun handleServerPacket(packet: Packet) {
                if(packet is RemoveBlockFromLevelPacket) {
                    // TODO
                }
            }

            override fun handleClientPacket(packet: Packet) {
            }

            override fun update() {
                selected.removeIf { !it.inLevel }
            }

            override fun updateCurrentlyActive() {
                currentlyActive = true
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
                            LevelManager.levelUnderMouse!!.getMovingObjectCollisionsFromPixelRectangle(x, y, w, h).toMutableSet()
                        }
                        SelectorMode.ALL -> {
                            (LevelManager.levelUnderMouse!!.getIntersectingBlocksFromPixelRectangle(x, y, w, h).toList() +
                                    LevelManager.levelUnderMouse!!.getMovingObjectCollisionsFromPixelRectangle(x, y, w, h)).toMutableSet()
                        }
                        SelectorMode.BLOCKS_ONLY -> {
                            LevelManager.levelUnderMouse!!.getIntersectingBlocksFromPixelRectangle(x, y, w, h).toMutableSet()
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
                    val obj = LevelManager.levelObjectUnderMouse
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