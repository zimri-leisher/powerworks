package level

import behavior.leaves.FindPath
import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag
import data.ConcurrentlyModifiableMutableList
import graphics.Image
import graphics.Renderer
import graphics.TextureRenderParams
import io.InputManager
import item.weapon.Projectile
import level.block.Block
import level.entity.robot.Robot
import level.entity.robot.RobotType
import level.particle.Particle
import level.tile.OreTileType
import level.tile.Tile
import main.DebugCode
import main.Game
import misc.Geometry
import misc.Numbers
import resource.ResourceNode
import routing.ResourceRoutingNetwork
import screen.elements.GUILevelView
import screen.mouse.Tool
import java.io.*
import java.util.*

const val CHUNK_SIZE_TILES = 8
val CHUNK_TILE_EXP = (Math.log(CHUNK_SIZE_TILES.toDouble()) / Math.log(2.0)).toInt()
val CHUNK_PIXEL_EXP = CHUNK_TILE_EXP + 4
val CHUNK_SIZE_PIXELS = CHUNK_SIZE_TILES shl 4

abstract class Level(
        @Tag(1)
        val levelInfo: LevelInfo) {

    @Tag(2)
    val widthTiles = levelInfo.settings.widthTiles
    @Tag(3)
    val heightTiles = levelInfo.settings.heightTiles
    @Tag(4)
    val heightPixels = heightTiles shl 4
    @Tag(5)
    val widthPixels = widthTiles shl 4
    @Tag(6)
    val heightChunks = heightTiles shr CHUNK_TILE_EXP
    @Tag(7)
    val widthChunks = widthTiles shr CHUNK_TILE_EXP

    @Tag(8)
    val seed: Long
    @Tag(9)
    val rand: Random

    val particles = ConcurrentlyModifiableMutableList<Particle>()
    @Tag(10)
    val chunks: Array<Chunk>

    val loadedChunks = ConcurrentlyModifiableMutableList<Chunk>()
    @Tag(11)
    val oreNoises = mutableMapOf<OreTileType, Noise>()

    @Tag(12)
    val projectiles = mutableListOf<Projectile>()

    init {
        if (!levelInfo.settings.empty) {
            LevelManager.allLevels.add(this)
            println("Loading level")
            seed = levelInfo.seed
            rand = Random(seed)
            for (t in OreTileType.ALL) {
                oreNoises.put(t, OpenSimplexNoise(Numbers.genRandom(seed, rand.nextInt(99).toLong())))
            }
            val gen = arrayOfNulls<Chunk>(widthChunks * heightChunks)
            for (y in 0 until heightChunks) {
                for (x in 0 until widthChunks) {
                    gen[x + y * widthChunks] = Chunk(this, x, y)
                }
            }
            chunks = gen.requireNoNulls()
        } else {
            seed = 0
            rand = Random(seed)
            chunks = arrayOf()
        }
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
        val maxX = Math.min(xTile1, widthTiles)
        val maxY = Math.min(yTile1, heightTiles)
        val minX = Math.max(xTile0, 0)
        val minY = Math.max(yTile0, 0)
        var count = 0
        for (y in (maxY - 1) downTo minY) {
            for (x in minX until maxX) {
                val c = getChunkFromTile(x, y)
                c.getTile(x, y).render()
                count++
            }
        }
        Tool.renderBelow()
        particles.forEach {
            it.render()
        }
        for (y in (maxY - 1) downTo minY) {
            val yChunk = y shr CHUNK_TILE_EXP
            // Render the line of blocks
            for (x in minX until maxX) {
                val c = getChunkAt(x shr CHUNK_TILE_EXP, yChunk)
                val b = c.getBlock(x, y)
                if (b != null) {
                    b.render()
                }
            }
            // Render the moving objects in sorted order
            for (xChunk in (minX shr CHUNK_TILE_EXP)..(maxX shr CHUNK_TILE_EXP)) {
                val c = getChunkAt(xChunk, yChunk)
                if (c.moving!!.size > 0)
                    c.moving!!.forEach {
                        if (it.yTile >= y && it.yTile < y + 1) {
                            it.render()
                        }
                    }
            }
        }

        ResourceRoutingNetwork.render()
        projectiles.forEach { it.render() }
        Tool.renderAbove()

        val chunksInTileRectangle = getChunksFromTileRectangle(minX, minY, maxX - minX - 1, maxY - minY - 1)
        for (c in chunksInTileRectangle) {
            for (nList in c.resourceNodes!!) {
                for (n in nList) {
                    renderNodeDebug(n)
                }
            }
        }
        if (Game.currentDebugCode == DebugCode.CHUNK_INFO) {
            for (c in chunksInTileRectangle) {
                Renderer.renderEmptyRectangle(c.xTile shl 4, c.yTile shl 4, CHUNK_SIZE_PIXELS, CHUNK_SIZE_PIXELS)
                Renderer.renderText(
                        "x: ${c.xChunk}, y: ${c.yChunk}\n" +
                                "updates required: ${c.updatesRequired!!.size}\n" +
                                "moving objects: ${c.moving!!.size} (${c.movingOnBoundary!!.size} on boundary)",
                        c.xTile shl 4, c.yTile shl 4)
            }
        }
        FindPath.render()
    }

    fun update() {
        ResourceRoutingNetwork.update()
        if (InputManager.inputsBeingPressed.contains("SPACE"))
            add(Robot(RobotType.STANDARD, LevelManager.mouseLevelXPixel, LevelManager.mouseLevelYPixel))
        loadedChunks.startTraversing()
        for (c in loadedChunks) {
            if (c.updatesRequired!!.size == 0 &&
                    c.movingOnBoundary!!.size == 0 &&
                    !c.beingRendered &&
                    c.resourceNodes!!.all {
                        it.isEmpty()
                    }) {
                c.unload()
            } else {
                c.update()
            }
        }
        loadedChunks.endTraversing()
        particles.forEach { it.update() }
        Tool.update()
    }

    // Most of these functions below are messy but they are only used here and only for simplifying boilerplate
    private fun renderNodeDebug(n: ResourceNode) {
        val xSign = Geometry.getXSign(n.dir)
        val ySign = Geometry.getYSign(n.dir)
        if (n.behavior.allowOut.possible() == null) {
            Renderer.renderTexture(Image.Misc.THIN_ARROW, (n.xTile shl 4) + 4 + 8 * xSign, (n.yTile shl 4) + 4 + 8 * ySign, TextureRenderParams(rotation = 90f * n.dir))
        }
        if (n.behavior.allowIn.possible() == null) {
            Renderer.renderTexture(Image.Misc.THIN_ARROW, (n.xTile shl 4) + 4 + 8 * xSign, (n.yTile shl 4) + 4 + 8 * ySign, TextureRenderParams(rotation = 90f * Geometry.getOppositeAngle(n.dir)))
        }
    }

    // Will I know what I did at 3:16 AM later? Hopefully. Right now this seems reasonable

    // older Zim says it's good enough and does the job. Because not used anywhere else, no real problems keeping this

    // even older Zim (18 yrs old now) says it turns out a lot of it got moved away anyways, so it worked perfectly as a temporary solution!

    // 19 now, and bam, its all gone to [LevelManager] or [LevelUtility]. 2:22 am mohican outdoor center

    /* Generation */
    abstract fun genTiles(xChunk: Int, yChunk: Int): Array<Tile>

    abstract fun genBlocks(xChunk: Int, yChunk: Int): Array<Block?>
}