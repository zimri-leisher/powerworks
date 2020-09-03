package screen.mouse

import com.badlogic.gdx.Input
import data.WeakMutableList
import graphics.Renderer
import graphics.text.TextRenderParams
import io.MouseMovementListener
import item.ItemType
import level.CHUNK_PIXEL_EXP
import level.LevelManager
import level.getResourceNodesAt
import level.pipe.PipeBlock
import main.DebugCode
import main.Game
import main.GameState
import main.toColor
import player.PlayerManager
import resource.ResourceContainer
import resource.ResourceContainerChangeListener
import resource.ResourceList
import routing.dist
import screen.ScreenManager

object Mouse : ResourceContainerChangeListener {

    val mouseMovementListeners = WeakMutableList<MouseMovementListener>()

    /**
     * Use Input.Buttons.<button name> from libgdx to check against this
     */
    var button = Input.Buttons.LEFT

    private var moved = false

    var xPixel = 0
        set(value) {
            if (field != value) {
                field = value
                moved = true
            }
        }
    var yPixel = 0
        set(value) {
            if (field != value) {
                field = value
                moved = true
            }
        }

    /**
     * The current item type being held. Used for movement between inventories, placing blocks and other interactions
     */
    var heldItemType: ItemType? = null

    fun update() {
        if (moved) {
            mouseMovementListeners.forEach { it.onMouseMove(xPixel, yPixel) }
        }
    }

    fun render() {
        if (heldItemType != null) {
            val itemType = heldItemType!!
            val quantity = PlayerManager.localPlayer.brainRobot.inventory.getQuantity(itemType)
            val type = itemType.icon
            type.render(xPixel, yPixel, 16, 16, true)
            Renderer.renderText(quantity, xPixel + 4, yPixel - 4)
        }
        when (Game.currentDebugCode) {
            DebugCode.TUBE_INFO -> {
                val t = LevelManager.levelObjectUnderMouse
                if (t is PipeBlock) {
                    val tubeString = "Tube:\n" +
                            "  Tile: ${t.xTile}, ${t.yTile}\n" +
                            "  Group: ${t.network.id}\n"
                    val intersection = t.network.intersections.firstOrNull { it.pipeBlock == t }
                    val intersectionString =
                            if (t.shouldBeIntersection() && intersection != null)
                                "Intersection connections:\n" +
                                        "  Up: ${intersection.connections[0]?.dist}\n" +
                                        "  Right: ${intersection.connections[1]?.dist}\n" +
                                        "  Down: ${intersection.connections[2]?.dist}\n" +
                                        "  Left: ${intersection.connections[3]?.dist}\n"
                            else if (t.shouldBeIntersection() && intersection == null)
                                "Should be intersection but hasn't been added"
                            else "Not intersection\n"
                    Renderer.renderText(tubeString + intersectionString, xPixel, yPixel, TextRenderParams(color = toColor(r = 255, g = 0, b = 0)))
                }
            }
            DebugCode.RESOURCE_NODES_INFO -> {
                val nodes = LevelManager.levelUnderMouse?.getResourceNodesAt(LevelManager.mouseLevelXPixel shr 4, LevelManager.mouseLevelYPixel shr 4)
                if (nodes != null) {
                    val s = StringBuilder()
                    for (n in nodes) {
                        s.append("    in: ${n.behavior.allowIn},       out: ${n.behavior.allowOut}\n" +
                                "    force in: ${n.behavior.forceIn}, forceOut: ${n.behavior.forceOut}\n" +
                                "    dir: ${n.dir}\n" +
                                "    container: ${n.attachedContainer}")
                    }
                    Renderer.renderText("Resource nodes at ${LevelManager.mouseLevelXPixel shr 4}, ${LevelManager.mouseLevelYPixel shr 4}:\n$s", xPixel, yPixel, TextRenderParams(color = toColor(r = 255, g = 0, b = 0)))

                }
            }
            DebugCode.SCREEN_INFO -> {
                Renderer.renderText("Element under mouse:\n" +
                        "  ${ScreenManager.elementUnderMouse}\n" +
                        "Window under mouse:\n" +
                        "  ${ScreenManager.guiUnderMouse}", xPixel + 3, yPixel + 3, TextRenderParams(color = toColor(r = 255, g = 0, b = 0)))
                Renderer.renderFilledRectangle(xPixel, yPixel, 1, 1)
            }
            DebugCode.POSITION_INFO -> {
                Renderer.renderText("Screen:\n" +
                        "  Pixel: $xPixel, $yPixel\n" +
                        if (GameState.currentState == GameState.INGAME) "Level:\n" +
                                "  Pixel: ${LevelManager.mouseLevelXPixel}, ${LevelManager.mouseLevelYPixel}\n" +
                                "  Tile: ${LevelManager.mouseLevelXPixel shr 4}, ${LevelManager.mouseLevelYPixel shr 4}\n" +
                                "  Chunk: ${LevelManager.mouseLevelXPixel shr CHUNK_PIXEL_EXP}, ${LevelManager.mouseLevelYPixel shr CHUNK_PIXEL_EXP}" else "", xPixel, yPixel, TextRenderParams(color = toColor(r = 255, g = 0, b = 0)))
            }
            DebugCode.LEVEL_INFO -> {
                Renderer.renderText("Level object under mouse:\n" +
                        "    Type: ${if(LevelManager.levelObjectUnderMouse != null) LevelManager.levelObjectUnderMouse!!::class.simpleName else ""} (${LevelManager.levelObjectUnderMouse?.type})\n" +
                        "    Id: ${LevelManager.levelObjectUnderMouse?.id}\n" +
                        "    Team: ${LevelManager.levelObjectUnderMouse?.team}\n" +
                        "    In level: ${LevelManager.levelObjectUnderMouse?.level?.id}", xPixel, yPixel, TextRenderParams(color = toColor(r = 255, g = 0, b = 0)))
            }
        }
    }

    override fun onContainerClear(container: ResourceContainer) {
    }

    override fun onAddToContainer(container: ResourceContainer, resources: ResourceList) {
        if (container.id == PlayerManager.localPlayer.brainRobot.inventory.id && heldItemType != null) {
            if (PlayerManager.localPlayer.brainRobot.inventory.getQuantity(heldItemType!!) == 0)
                heldItemType = null
        }
    }

    override fun onRemoveFromContainer(container: ResourceContainer, resources: ResourceList) {
        if (container.id == PlayerManager.localPlayer.brainRobot.inventory.id && heldItemType != null) {
            if (PlayerManager.localPlayer.brainRobot.inventory.getQuantity(heldItemType!!) == 0)
                heldItemType = null
        }
    }
}
