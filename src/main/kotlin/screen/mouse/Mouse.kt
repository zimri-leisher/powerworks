package screen.mouse

import graphics.Image
import graphics.Renderer
import graphics.Texture
import io.*
import item.ItemType
import level.CHUNK_PIXEL_EXP
import level.DroppedItem
import level.Level
import level.LevelObject
import level.pipe.PipeBlock
import level.tube.TubeBlock
import main.DebugCode
import main.Game
import main.State
import resource.ResourceContainer
import resource.ResourceContainerChangeListener
import resource.ResourceType
import screen.HUD
import screen.ScreenManager
import screen.elements.*

object Mouse : ControlPressHandler, ResourceContainerChangeListener {
    const val DROPPED_ITEM_PICK_UP_RANGE = 8

    const val ICON_SIZE = 8

    /**
     * 1: left mouse button
     * 2: middle mouse button
     * 3: right mouse button
     */
    var button = 0

    var xPixel = 0
    var yPixel = 0

    /**
     * The current item type being held. Used for movement between inventories, placing blocks and other interactions
     */
    var heldItemType: ItemType? = null

    internal var window = GUIWindow("Mouse", { xPixel + 4 }, { yPixel }, { 0 }, { 0 }, ScreenManager.Groups.MOUSE, true, 0).apply {
        transparentToInteraction = true
    }
    private var group = GUIGroup(window, "Mouse info group", { 0 }, { 0 }, open = true).apply {
        transparentToInteraction = true
    }

    internal var text = GUIText(group, "Mouse info group text", 2, 2, "", layer = group.layer + 3).apply {
        open = false
    }

    internal var background = GUIDefaultTextureRectangle(group, "Mouse info group background", { 0 }, { 0 }, { text.widthPixels + 4 }, { text.heightPixels + 4 }, layer = group.layer + 2).apply {
        open = false
    }

    private var icon = GUITexturePane(group, "Mouse icon", 0, 0, Image.Misc.ERROR, ICON_SIZE, ICON_SIZE).apply {
        open = false
        updateDimensionAlignmentOnTextureChange = false
    }

    init {
        InputManager.registerControlPressHandler(this, ControlPressHandlerType.GLOBAL, Control.DROP_HELD_ITEM, Control.PICK_UP_DROPPED_ITEMS)
    }

    /**
     * An icon that renders under the cursor, for example, the teleportation icon
     */
    fun setSecondaryIcon(texture: Texture) {
        icon.texture = texture
        icon.open = true
    }

    fun clearSecondaryIcon() {
        icon.open = false
    }

    internal fun render() {
        if (heldItemType != null) {
            val i = heldItemType!!
            val q = Game.mainInv.getQuantity(i)
            val t = i.icon
            Renderer.renderTextureKeepAspect(t, xPixel + 4, yPixel, GUIItemSlot.WIDTH, GUIItemSlot.HEIGHT)
            Renderer.renderText(q, xPixel + 4, yPixel)
        }
        when(Game.currentDebugCode) {
            DebugCode.TUBE_INFO -> {
                val t = Game.currentLevel.selectedLevelObject
                if (t is TubeBlock) {
                    val tubeString = "Tube:\n" +
                            "  Tile: ${t.xTile}, ${t.yTile}\n" +
                            "  Group: ${t.group.id}\n"
                    val intersection = t.group.intersections.firstOrNull { it.tubeBlock == t }
                    val intersectionString =
                            if (t.group.isIntersection(t) == true && intersection != null)
                                "Intersection connections:\n" +
                                        "  Up: ${intersection.connectedTo[0]?.dist}\n" +
                                        "  Right: ${intersection.connectedTo[1]?.dist}\n" +
                                        "  Down: ${intersection.connectedTo[2]?.dist}\n" +
                                        "  Left: ${intersection.connectedTo[3]?.dist}\n"
                            else if (t.group.isIntersection(t) && intersection == null)
                                "Should be intersection but hasn't been added"
                            else "Not intersection\n"
                    Renderer.renderText(tubeString + intersectionString, xPixel, yPixel)
                }
            }
            DebugCode.PIPE_INFO -> {
                val t = Game.currentLevel.selectedLevelObject
                if (t is PipeBlock) {
                    val pipeString = "Tube:\n" +
                            "  Tile: ${t.xTile}, ${t.yTile}\n" +
                            "  Group: ${t.group.id}\n" +
                            "     Size: ${t.group.size}"
                    Renderer.renderText(pipeString, xPixel, yPixel)
                }
            }
            DebugCode.RESOURCE_NODES_INFO -> {
                val nodes = Level.ResourceNodes.get(Game.currentLevel.mouseLevelXPixel shr 4, Game.currentLevel.mouseLevelYPixel shr 4)
                val s = StringBuilder()
                for (n in nodes) {
                    s.append("    in: ${n.allowIn}, out: ${n.allowOut}, dir: ${n.dir}\n")
                }
                Renderer.renderText("Resource nodes at ${Game.currentLevel.mouseLevelXPixel shr 4}, ${Game.currentLevel.mouseLevelYPixel shr 4}:\n$s", xPixel, yPixel)
            }
            DebugCode.SCREEN_INFO -> {
                Renderer.renderText("Element on mouse:\n" +
                        "  ${ScreenManager.getHighestElement(xPixel, yPixel, predicate = { !it.transparentToInteraction })}\n" +
                        "Window on mouse:\n" +
                        "  ${ScreenManager.getHighestWindow(xPixel, yPixel, { !it.transparentToInteraction })}", xPixel, yPixel)
            }
            DebugCode.POSITION_INFO -> {
                Renderer.renderText("Screen:\n" +
                        "  Pixel: $xPixel, $yPixel\n" +
                        "  Tile: ${xPixel shr 4}, ${yPixel shr 4}\n" +
                        "  Chunk: ${xPixel shr CHUNK_PIXEL_EXP}, ${yPixel shr CHUNK_PIXEL_EXP}\n" +
                        if (State.CURRENT_STATE == State.INGAME) "Level:\n" +
                                "  Pixel: ${Game.currentLevel.mouseLevelXPixel}, ${Game.currentLevel.mouseLevelYPixel}\n" +
                                "  Tile: ${Game.currentLevel.mouseLevelXPixel shr 4}, ${Game.currentLevel.mouseLevelYPixel shr 4}\n" +
                                "  Chunk: ${Game.currentLevel.mouseLevelXPixel shr CHUNK_PIXEL_EXP}, ${Game.currentLevel.mouseLevelYPixel shr CHUNK_PIXEL_EXP}" else "", xPixel, yPixel)
            }
        }
    }

    /**
     * Tries to place the held item on the level
     * @param q how many to drop
     */
    private fun dropHeldItem(q: Int = 1) {
        if (heldItemType != null) {
            val type = heldItemType!!
            if (Level.add(DroppedItem(Game.currentLevel.mouseLevelXPixel, Game.currentLevel.mouseLevelYPixel, type, q)))
                Game.mainInv.remove(type, q)
        }
    }

    override fun onContainerClear(container: ResourceContainer<*>) {
    }

    override fun onContainerChange(container: ResourceContainer<*>, resource: ResourceType, quantity: Int) {
        if (container == Game.mainInv && heldItemType != null) {
            if (Game.mainInv.getQuantity(heldItemType!!) == 0)
                heldItemType = null
        }
    }

    override fun handleControlPress(p: ControlPress) {
        if (p.pressType == PressType.PRESSED) {
            when (p.control) {
                Control.DROP_HELD_ITEM -> dropHeldItem()
                Control.PICK_UP_DROPPED_ITEMS -> {
                    val i = Level.DroppedItems.getInRadius(Game.currentLevel.mouseLevelXPixel, Game.currentLevel.mouseLevelYPixel, DROPPED_ITEM_PICK_UP_RANGE)
                    if (i.isNotEmpty()) {
                        val g = i.first()
                        if (!Game.mainInv.full) {
                            Game.mainInv.add(g.itemType, g.quantity)
                            Level.remove(g)
                            if (heldItemType == null) {
                                heldItemType = g.itemType
                            }
                            HUD.Hotbar.items.add(g.itemType)
                        }
                    }
                }
            }
        }
    }
}
