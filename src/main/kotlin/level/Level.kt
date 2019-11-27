package level

import behavior.leaves.FindPath
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import graphics.Image
import graphics.Renderer
import graphics.TextureRenderParams
import io.InputManager
import level.block.Block
import level.entity.robot.Robot
import level.entity.robot.RobotType
import level.moving.MovingObject
import level.tile.Tile
import main.DebugCode
import main.Game
import misc.Geometry
import network.ClientNetworkManager
import network.ServerNetworkManager
import network.User
import routing.ResourceRoutingNetwork
import screen.elements.GUILevelView
import screen.mouse.Tool
import java.util.*

const val CHUNK_SIZE_TILES = 8
val CHUNK_TILE_EXP = (Math.log(CHUNK_SIZE_TILES.toDouble()) / Math.log(2.0)).toInt()
val CHUNK_PIXEL_EXP = CHUNK_TILE_EXP + 4
val CHUNK_SIZE_PIXELS = CHUNK_SIZE_TILES shl 4

/**
 * A level consists of the [Tile]s, [Block]s, [MovingObject]s and various other things that make up the actual gameplay.
 *
 * Loading, saving and other general tasks relating to [Level]s are done by the [LevelManager].
 *
 * [Level]s have various utility methods defined in the `LevelUtlity.kt` file as extension methods. These should be used
 * over direct access to chunks and their [LevelObject]s.
 *
 * [ActualLevel]s, which are usually on a central [ServerNetworkManager], represent the real, true data, whereas [RemoteLevel]s are only
 * copies for the [ClientNetworkManager] to have. [RemoteLevel]s are trivial to create and don't actually generate or load anything on their own.
 *
 * @param info the information describing the [Level]
 */
abstract class Level(
        val id: UUID,
        /**
         * The information describing this level. It defines the username of the owner of this level, the name of the level,
         * the generation settings and other related things (see [LevelInfo] for more detail). Two levels with the same [info] should be identically generated
         */
        val info: LevelInfo) {

    val generator = info.levelType.getGenerator(this)

    abstract var data: LevelData
        protected set

    val widthChunks = info.levelType.widthChunks
    val heightChunks = info.levelType.heightChunks

    val widthTiles = widthChunks shl CHUNK_TILE_EXP
    val heightTiles = widthChunks shl CHUNK_TILE_EXP

    val heightPixels = heightChunks shl CHUNK_PIXEL_EXP
    val widthPixels = widthChunks shl CHUNK_PIXEL_EXP

    init {
        LevelManager.allLevels.add(this)
    }

    fun update() {
        ResourceRoutingNetwork.update()
        if (InputManager.inputsBeingPressed.contains("SPACE"))
            add(Robot(RobotType.STANDARD, LevelManager.mouseLevelXPixel, LevelManager.mouseLevelYPixel))
        data.chunks.forEach { it.update() }
        data.particles.forEach { it.update() }
        Tool.update()
    }

    fun render(view: GUILevelView) {
        // Assume it is already added to views list
        val r = view.viewRectangle
        val xPixel0 = r.minX.toInt()
        val yPixel0 = r.minY.toInt()
        val xPixel1 = r.maxX.toInt()
        val yPixel1 = r.maxY.toInt()
        val xTile0 = xPixel0 shr 4
        val yTile0 = yPixel0 shr 4
        val xTile1 = (xPixel1 shr 4) + 1
        val yTile1 = (yPixel1 shr 4) + 1
        val maxX = xTile1.coerceAtMost(widthTiles)
        val maxY = yTile1.coerceAtMost(heightTiles)
        val minX = xTile0.coerceAtLeast(0)
        val minY = yTile0.coerceAtLeast(0)
        for (y in (maxY - 1) downTo minY) {
            for (x in minX until maxX) {
                val c = getChunkFromTile(x, y)
                c.getTile(x, y).render()
            }
        }
        Tool.renderBelow()
        data.particles.forEach {
            it.render()
        }
        for (y in (maxY - 1) downTo minY) {
            val yChunk = y shr CHUNK_TILE_EXP
            // Render the line of blocks
            for (x in minX until maxX) {
                val c = getChunkAt(x shr CHUNK_TILE_EXP, yChunk)
                c.getBlock(x, y)?.render()
            }
            // Render the moving objects in sorted order
            for (xChunk in (minX shr CHUNK_TILE_EXP)..(maxX shr CHUNK_TILE_EXP)) {
                val c = getChunkAt(xChunk, yChunk)
                if (c.data.moving.size > 0)
                    c.data.moving.forEach {
                        if (it.yTile >= y && it.yTile < y + 1) {
                            it.render()
                        }
                    }
            }
        }

        ResourceRoutingNetwork.render()
        data.projectiles.forEach { it.render() }
        Tool.renderAbove()

        val chunksInTileRectangle = getChunksFromTileRectangle(minX, minY, maxX - minX - 1, maxY - minY - 1)
        for (c in chunksInTileRectangle) {
            for (nList in c.data.resourceNodes) {
                for (n in nList) {
                    val xSign = Geometry.getXSign(n.dir)
                    val ySign = Geometry.getYSign(n.dir)
                    if (n.behavior.allowOut.possible() == null) {
                        Renderer.renderTexture(Image.Misc.THIN_ARROW, (n.xTile shl 4) + 4 + 8 * xSign, (n.yTile shl 4) + 4 + 8 * ySign, TextureRenderParams(rotation = 90f * n.dir))
                    }
                    if (n.behavior.allowIn.possible() == null) {
                        Renderer.renderTexture(Image.Misc.THIN_ARROW, (n.xTile shl 4) + 4 + 8 * xSign, (n.yTile shl 4) + 4 + 8 * ySign, TextureRenderParams(rotation = 90f * Geometry.getOppositeAngle(n.dir)))
                    }
                }
            }
        }

        if (Game.currentDebugCode == DebugCode.CHUNK_INFO) {
            for (c in chunksInTileRectangle) {
                Renderer.renderEmptyRectangle(c.xTile shl 4, c.yTile shl 4, CHUNK_SIZE_PIXELS, CHUNK_SIZE_PIXELS)
                Renderer.renderText(
                        "x: ${c.xChunk}, y: ${c.yChunk}\n" +
                                "updates required: ${c.data.updatesRequired.size}\n" +
                                "moving objects: ${c.data.moving.size} (${c.data.movingOnBoundary.size} on boundary)",
                        c.xTile shl 4, c.yTile shl 4)
            }
        }

        FindPath.render()
    }

    // Most of these functions below are messy but they are only used here and only for simplifying boilerplate // ha lol theyre all gone

    // Will I know what I did at 3:16 AM later? Hopefully. Right now this seems reasonable

    // older Zim says it's good enough and does the job. Because not used anywhere else, no real problems keeping this

    // even older Zim (18 yrs old now) says it turns out a lot of it got moved away anyways, so it worked perfectly as a temporary solution!

    // 19 now, and bam, its all gone to [LevelManager] or [LevelUtility]. 2:22 am mohican outdoor center
}

class NonexistentLevelException(message: String) : Exception(message)

class LevelSerializer : Serializer<Level>() {

    override fun write(kryo: Kryo, output: Output, `object`: Level) {
        kryo.writeObject(output, `object`.id)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out Level>): Level {
        val id = kryo.readObject(input, UUID::class.java)
        var level = LevelManager.allLevels.firstOrNull { it.id == id }
        if(level == null) {
            level = ActualLevel(id, LevelManager.loadLevelInfo(id) ?: throw NonexistentLevelException("No previously existing level (id: $id) exists"))
        }
        kryo.reference(level)
        return level
    }
}