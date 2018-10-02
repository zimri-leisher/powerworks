package screen.mouse

import com.badlogic.gdx.graphics.Color
import graphics.Renderer
import graphics.TextureRenderParams
import io.*
import level.Level
import level.block.Block
import level.block.BlockType
import level.block.GhostBlock
import level.moving.MovingObject
import main.Game

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
     * This is only called if the tool is active
     */
    open fun render() {}

    /**
     * This is only called if the tool is active
     */
    open fun update() {}

    companion object {

        val ALL = mutableListOf<Tool>()

        val ALL_ACTIVE = mutableListOf<Tool>()

        val INTERACTOR = object : Tool(Control.Group.INTERACTION) {
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

            override fun render() {
                val s = Game.currentLevel.selectedLevelObject!!
                if (s is Block)
                    Renderer.renderEmptyRectangle(s.xPixel, s.yPixel, s.type.widthTiles shl 4, s.type.heightTiles shl 4, params = TextureRenderParams(color = Color(0x1A6AF472)))
                else if (s is MovingObject)
                    Renderer.renderEmptyRectangle(s.xPixel, s.yPixel, s.hitbox.width, s.hitbox.height, params = TextureRenderParams(color = Color(0x1A6AF472)))
            }
        }

        val BLOCK_REMOVER = object : Tool(Control.REMOVE_BLOCK) {
            override fun updateCurrentlyActive() {
                currentlyActive = Game.currentLevel.selectedLevelObject != null
            }

            override fun onUse(control: Control, type: PressType, mouseLevelXPixel: Int, mouseLevelYPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
                if (currentlyActive) {
                    if (type == PressType.PRESSED) {
                        val o = Game.currentLevel.selectedLevelObject!!
                        Level.remove(o)
                    }
                }
            }
        }

        val BLOCK_PLACER: Tool = object : Tool(Control.PLACE_BLOCK, Control.ROTATE_BLOCK), ControlPressHandler {

            override fun onUse(control: Control, type: PressType, mouseLevelXPixel: Int, mouseLevelYPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
                if (currentlyActive) {
                    if (type == PressType.PRESSED) {
                        if (control == Control.ROTATE_BLOCK) {
                            Game.currentLevel.ghostBlockRotation = (Game.currentLevel.ghostBlockRotation + 1) % 4
                        }
                    }
                    if (type == PressType.RELEASED) {
                        if (control == Control.PLACE_BLOCK) {
                            val block = Game.currentLevel.selectedLevelObject
                            if (block == null) {
                                if (Game.currentLevel.ghostBlock != null) {
                                    val gBlock = Game.currentLevel.ghostBlock!!
                                    if (Level.add(gBlock.type.instantiate(gBlock.xPixel, gBlock.yPixel, gBlock.rotation))) {
                                        val h = Mouse.heldItemType!!
                                        Game.mainInv.remove(h, 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            override fun updateCurrentlyActive() {
                currentlyActive = Game.currentLevel.selectedLevelObject == null
            }

            override fun update() {
                val currentItem = Mouse.heldItemType
                if (currentItem == null || currentItem.placedBlock == BlockType.ERROR || (Game.mainInv.getQuantity(currentItem) == 0)) {
                    if (Game.currentLevel.ghostBlock != null)
                        Level.remove(Game.currentLevel.ghostBlock!!)
                    Game.currentLevel.ghostBlock = null
                } else if (currentItem.placedBlock != BlockType.ERROR) {
                    val placedType = currentItem.placedBlock
                    val xTile = ((Game.currentLevel.mouseLevelXPixel) shr 4) - placedType.widthTiles / 2
                    val yTile = ((Game.currentLevel.mouseLevelYPixel) shr 4) - placedType.heightTiles / 2
                    Game.currentLevel.ghostBlock = GhostBlock(placedType, xTile, yTile, Game.currentLevel.ghostBlockRotation)
                }
            }

            override fun render() {
                if (Game.currentLevel.ghostBlock != null) {
                    val g = Game.currentLevel.ghostBlock!!
                    g.render()
                }
            }
        }

        val TELEPORTER = object : Tool(Control.TELEPORT) {

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

        val SELECTOR = object : Tool(Control.Group.SELECTOR_TOOLS) {

            var dragging = false
            var dragStartXPixel = 0
            var dragStartYPixel = 0
            var currentDragXPixel = 0
            var currentDragYPixel = 0

            var mode = SelectorMode.ALL

            override fun update() {

            }

            override fun updateCurrentlyActive() {
                currentlyActive = dragging
            }

            override fun onUse(control: Control, type: PressType, mouseLevelXPixel: Int, mouseLevelYPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
                if (type == PressType.PRESSED) {
                    dragStartXPixel = mouseLevelXPixel
                    dragStartYPixel = mouseLevelYPixel
                    currentDragXPixel = mouseLevelXPixel
                    currentDragYPixel = mouseLevelYPixel
                } else if (type == PressType.REPEAT) {
                    currentDragXPixel = mouseLevelXPixel
                    currentDragYPixel = mouseLevelYPixel
                    if (Math.abs(dragStartXPixel - currentDragXPixel) > 2 || Math.abs(dragStartYPixel - currentDragYPixel) > 2) {
                        dragging = true
                    }
                } else if (type == PressType.RELEASED) {
                    dragging = false
                }
            }

            override fun render() {
                Renderer.renderEmptyRectangle(dragStartXPixel, dragStartYPixel, (currentDragXPixel - dragStartXPixel), (currentDragYPixel - dragStartYPixel), params = TextureRenderParams(color = Color(0xBBBBBB)))
            }
        }

        val DEBUG = object : Tool(Control.Group.INTERACTION) {

            override fun onUse(control: Control, type: PressType, mouseLevelXPixel: Int, mouseLevelYPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
                if (currentlyActive) {
                    if (type == PressType.PRESSED) {
                        Level.Tiles.get(mouseLevelXPixel shr 4, mouseLevelYPixel shr 4).apply {
                            println("Test")
                            rotation = (rotation + 1) % 4
                        }
                    }
                }
            }

            override fun updateCurrentlyActive() {
                currentlyActive = false
            }

            override fun render() {
                Renderer.renderText(Level.Tiles.get(Game.currentLevel.mouseLevelXPixel shr 4, Game.currentLevel.mouseLevelYPixel shr 4).rotation, ((Game.currentLevel.mouseLevelXPixel shr 4) shl 4) + 4, ((Game.currentLevel.mouseLevelYPixel shr 4) shl 4) + 4)
            }

        }

        fun render() {
            ALL_ACTIVE.forEach { it.render() }
        }

        fun update() {
            ALL.forEach { it.updateCurrentlyActive() }
            ALL_ACTIVE.forEach { it.update() }
        }
    }
}