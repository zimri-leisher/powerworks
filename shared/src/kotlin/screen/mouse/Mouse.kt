package screen.mouse

import com.badlogic.gdx.Input
import data.WeakMutableList
import graphics.Renderer
import graphics.text.TextRenderParams
import io.MouseMovementListener
import item.ItemType
import level.CHUNK_EXP
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
import screen.ScreenManager

object Mouse : ResourceContainerChangeListener {

    val mouseMovementListeners = WeakMutableList<MouseMovementListener>()

    /**
     * Use Input.Buttons.<button name> from libgdx to check against this
     */
    var button = Input.Buttons.LEFT

    private var moved = false

    var x = 0
        set(value) {
            if (field != value) {
                field = value
                moved = true
            }
        }
    var y = 0
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
            mouseMovementListeners.forEach { it.onMouseMove(x, y) }
        }
    }

    fun render() {
        if (heldItemType != null) {
            val itemType = heldItemType!!
            val quantity = PlayerManager.localPlayer.brainRobot.inventory.getQuantity(itemType)
            val type = itemType.icon
            type.render(x, y, 16, 16, true)
            Renderer.renderText(quantity, x + 4, y - 4)
        }
        when (Game.currentDebugCode) {
            DebugCode.PIPE_INFO -> {
                val t = LevelManager.levelObjectUnderMouse
                if (t is PipeBlock) {
                    val tubeString = "Tube:\n" +
                            "  Tile: ${t.xTile}, ${t.yTile}\n" +
                            "  Group: ${t.network?.id}\n"
                    val intersectionString = "Far:\n   Up: ${t.farEdges[0]}\n   Right: ${t.farEdges[1]}\n   Down: ${t.farEdges[2]}\n   Left: ${t.farEdges[3]}\n" +
                            "Near:\n   Up: ${t.edges[0]}\n   Right: ${t.edges[1]}\n   Down: ${t.edges[2]}\n   Left: ${t.edges[3]}\n"
                    Renderer.renderText(tubeString + intersectionString, x, y, TextRenderParams(color = toColor(r = 255, g = 0, b = 0)))
                }
            }
            DebugCode.RESOURCE_NODES_INFO -> {
                val nodes = LevelManager.levelUnderMouse?.getResourceNodesAt(LevelManager.mouseLevelX shr 4, LevelManager.mouseLevelY shr 4)
                if (nodes != null) {
                    val s = StringBuilder()
                    for (n in nodes) {
                        s.append("    in: ${n.behavior.allowIn},       out: ${n.behavior.allowOut}\n" +
                                "    force in: ${n.behavior.forceIn}, forceOut: ${n.behavior.forceOut}\n" +
                                "    dir: ${n.dir}\n" +
                                "    container: ${n.attachedContainer}")
                    }
                    Renderer.renderText("Resource nodes at ${LevelManager.mouseLevelX shr 4}, ${LevelManager.mouseLevelY shr 4}:\n$s", x, y, TextRenderParams(color = toColor(r = 255, g = 0, b = 0)))
                }
            }
            DebugCode.SCREEN_INFO -> {
                Renderer.renderText("Element under mouse:\n" +
                        "  ${ScreenManager.elementUnderMouse}\n" +
                        "Window under mouse:\n" +
                        "  ${ScreenManager.guiUnderMouse}", x + 3, y + 3, TextRenderParams(color = toColor(r = 255, g = 0, b = 0)))
                Renderer.renderFilledRectangle(x, y, 1, 1)
            }
            DebugCode.POSITION_INFO -> {
                Renderer.renderText("Screen:\n" +
                        "  Coord: $x, $y\n" +
                        if (GameState.currentState == GameState.INGAME) "Level:\n" +
                                "  Coord: ${LevelManager.mouseLevelX}, ${LevelManager.mouseLevelY}\n" +
                                "  Tile: ${LevelManager.mouseLevelX shr 4}, ${LevelManager.mouseLevelY shr 4}\n" +
                                "  Chunk: ${LevelManager.mouseLevelX shr CHUNK_EXP}, ${LevelManager.mouseLevelY shr CHUNK_EXP}" else "", x, y, TextRenderParams(color = toColor(r = 255, g = 0, b = 0)))
            }
            DebugCode.LEVEL_INFO -> {
                Renderer.renderText("Level object under mouse:\n" +
                        "    Type: ${if(LevelManager.levelObjectUnderMouse != null) LevelManager.levelObjectUnderMouse!!::class.simpleName else ""} (${LevelManager.levelObjectUnderMouse?.type})\n" +
                        "    Id: ${LevelManager.levelObjectUnderMouse?.id}\n" +
                        "    Team: ${LevelManager.levelObjectUnderMouse?.team}\n" +
                        "    In level: ${LevelManager.levelObjectUnderMouse?.level?.id}", x, y, TextRenderParams(color = toColor(r = 255, g = 0, b = 0)))
            }

            else -> {}
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
