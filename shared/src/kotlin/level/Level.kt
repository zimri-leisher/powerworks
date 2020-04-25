package level

import behavior.leaves.FindPath
import graphics.Image
import graphics.Renderer
import graphics.TextureRenderParams
import item.weapon.Projectile
import level.block.Block
import level.entity.robot.BrainRobot
import level.generator.EmptyLevelGenerator
import level.moving.MovingObject
import level.particle.Particle
import level.pipe.PipeBlock
import level.tile.Tile
import main.DebugCode
import main.Game
import main.toColor
import misc.Geometry
import network.ClientNetworkManager
import network.ServerNetworkManager
import resource.ResourceNode
import routing.PipeRoutingNetwork
import routing.ResourceRoutingNetwork
import screen.elements.GUILevelView
import screen.mouse.Mouse
import screen.mouse.Tool
import serialization.Input
import serialization.Output
import serialization.Serializer
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

    var loaded = false
        set(value) {
            if (field != value) {
                field = value
                if (field && generator !is EmptyLevelGenerator) {
                    LevelManager.loadedLevels.add(this)
                } else if (!field) {
                    LevelManager.loadedLevels.remove(this)
                }
            }
        }

    init {
        if (generator !is EmptyLevelGenerator) {
            LevelManager.allLevels.forEach {
                if (it.id == id) {
                    throw IllegalArgumentException("A level with id $id already exists ($it)")
                }
            }
            LevelManager.allLevels.add(this)
        }
    }

    /**
     * Does everything necessary to put the object to the level, if possible. If this is already in another
     * [Level], it will remove it from that level first.
     * @return true if the object was not already present and the object was added successfully
     */
    open fun add(l: LevelObject): Boolean {
        if (!canAdd(l)) {
            return false
        }
        if (l.level != this && l.inLevel) { // if already in another level
            l.level.remove(l)
        }
        if (l is Block) {
            for (x in 0 until l.type.widthTiles) {
                for (y in 0 until l.type.heightTiles) {
                    getChunkFromTile(l.xTile + x, l.yTile + y).setBlock(l, l.xTile + x, l.yTile + y, (x == 0 && y == 0))
                }
            }
            l.level = this
            l.inLevel = true
            return true
        } else if (l is MovingObject) {
            if (l.hitbox != Hitbox.NONE) {
                l.intersectingChunks.forEach { it.data.movingOnBoundary.add(l) }
            }
            l.level = this
            l.inLevel = true
            if (l is BrainRobot) {
                data.brainRobots.add(l)
            }
            return true
        }
        return false
    }

    /**
     * Forcibly adds [l] to the level, ignoring collisions and removing things that are in its place. This will always call
     * [LevelObject.onAddToLevel], regardless of whether it is already in the [Level] or not. If this is already in another
     * [Level], it will remove it from that level first
     */
    fun forceAdd(l: LevelObject) {
        if (l.level != this && l.inLevel) { // if already in another level
            l.level.remove(l)
        }
        if (l is Block) {
            l.getCollisions(l.xPixel, l.yPixel, level = this).forEach { remove(it) }
            for (x in 0 until l.type.widthTiles) {
                for (y in 0 until l.type.heightTiles) {
                    getChunkFromTile(l.xTile + x, l.yTile + y).setBlock(l, l.xTile + x, l.yTile + y, (x == 0 && y == 0))
                }
            }
        } else if (l is MovingObject) {
            if (l.hitbox != Hitbox.NONE) {
                l.intersectingChunks.forEach { it.data.movingOnBoundary.add(l) }
                if (l is DroppedItem) {
                    getChunkAt(l.xChunk, l.yChunk).addDroppedItem(l)
                }
            }
            if (l is BrainRobot) {
                data.brainRobots.add(l)
            }
        }
        if (!l.inLevel) {
            l.level = this
            l.inLevel = true
        } else {
            if (l.level == this) {
                l.onAddToLevel()
            } else {
                l.level = this
            }
        }
    }

    /**
     * Does everything necessary to remove [l] from this level
     *
     * @return true if [l] was in this level before calling this method, and now is no longer
     */
    open fun remove(l: LevelObject): Boolean {
        if (l.inLevel && l.level == this) {
            if (l is Block) {
                for (x in 0 until l.type.widthTiles) {
                    for (y in 0 until l.type.heightTiles) {
                        getChunkFromTile(l.xTile + x, l.yTile + y).removeBlock(l, l.xTile + x, l.yTile + y, (x == 0 && y == 0))
                    }
                }
                l.inLevel = false
                return true
            } else if (l is MovingObject) {
                val chunk = getChunkAt(l.xChunk, l.yChunk)
                if (l is DroppedItem) {
                    chunk.removeDroppedItem(l)
                }
                if (l.hitbox != Hitbox.NONE)
                    l.intersectingChunks.forEach { it.data.movingOnBoundary.remove(l) }
                chunk.removeMoving(l)
                l.inLevel = false
                if (l is BrainRobot) {
                    data.brainRobots.remove(l)
                }
            }
            return true
        }
        return false
    }

    /**
     * Adds a particle to the level. Particles are temporary and purely decorative, they do not get saved
     */
    open fun add(p: Particle) {
        p.level = this
        data.particles.add(p)
    }

    /**
     * Removes a particle from the level
     */
    open fun remove(p: Particle) {
        p.level = LevelManager.EMPTY_LEVEL
        data.particles.remove(p)
    }

    /**
     * Tries to add a resource node to the level. If [r] was already in another level before addition, it will remove it from
     * that level first.
     * If there was already a node at the same position with the same direction, attached container and resource
     * category, it will remove the previous one before finishing addition.
     *
     * @return true if the node was added successfully (is now in this level)
     */
    open fun add(r: ResourceNode): Boolean {
        if (r.inLevel && r.level != this) {
            r.level.remove(r)
        }
        val c = getChunkFromTile(r.xTile, r.yTile)
        val previousNode: ResourceNode?
        previousNode = c.data.resourceNodes[r.resourceCategory.ordinal].firstOrNull {
            it.xTile == r.xTile && it.yTile == r.yTile && it.dir == r.dir && it.attachedContainer == r.attachedContainer
        }
        if (previousNode != null) {
            remove(previousNode)
        }
        if (!c.addResourceNode(r)) {
            return false
        }
        r.level = this
        r.inLevel = true
        updateResourceNodeAttachments(r)
        for (x in -1..1) {
            for (y in -1..1) {
                if (Math.abs(x) != Math.abs(y))
                    getResourceNodesAt(r.xTile + x, r.yTile + y).forEach { updateResourceNodeAttachments(it) }
            }
        }
        return true
    }

    /**
     * Tries to remove a resource node [r] from the level. Does nothing if [r] was in a different level or not in one at all.
     *
     * @return true if, at the start, [r] was in this level and now it is not
     */
    open fun remove(r: ResourceNode): Boolean {
        if (!r.inLevel || r.level != this)
            return false
        val c = getChunkFromTile(r.xTile, r.yTile)
        if (!c.removeResourceNode(r)) {
            return false
        }
        r.inLevel = false
        for (x in -1..1) {
            for (y in -1..1) {
                if (Math.abs(x) != Math.abs(y))
                    getResourceNodesAt(r.xTile + x, r.yTile + y).forEach { updateResourceNodeAttachments(it) }
            }
        }
        return true
    }

    /**
     * Adds a [Projectile] to this [Level]
     * (no return needed, as you should always be able to add a projectile)
     */
    open fun add(projectile: Projectile) {
        data.projectiles.add(projectile)
    }

    /**
     * Removes a [Projectile] from this [Level]
     *
     * @return false if that projectile was not in this level
     */
    open fun remove(projectile: Projectile) = data.projectiles.remove(projectile)

    fun update() {
        ResourceRoutingNetwork.update()
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
                    if (n.behavior.allowOut.possible() != null) {
                        Renderer.renderTexture(Image.Misc.THIN_ARROW, (n.xTile shl 4) + 4 + 8 * xSign, (n.yTile shl 4) + 4 + 8 * ySign, TextureRenderParams(rotation = 90f * n.dir))
                    }
                    if (n.behavior.allowIn.possible() != null) {
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
        } else if (Game.currentDebugCode == DebugCode.TUBE_INFO) {
            val pipeUnderMouse = LevelManager.levelObjectUnderMouse as? PipeBlock
            if (pipeUnderMouse != null) {
                val group = pipeUnderMouse.network
                for (pipe in group.pipes) {
                    Renderer.renderFilledRectangle(pipe.xPixel + 4, pipe.yPixel + 4, 8, 8, TextureRenderParams(color = toColor(r = 1f, g = 0f, b = 0f)))
                }
            }
            val nodeUnderMouse = getResourceNodesAt(LevelManager.mouseLevelXTile, LevelManager.mouseLevelYTile).firstOrNull()
            if (nodeUnderMouse != null) {
                val network = nodeUnderMouse.network
                if (network is PipeRoutingNetwork) {
                    for (pipe in network.pipes) {
                        Renderer.renderFilledRectangle(pipe.xPixel + 4, pipe.yPixel + 4, 8, 8, TextureRenderParams(color = toColor(r = 1f, g = 0f, b = 0f)))
                    }
                }
            }
        }

        FindPath.render()
    }

    // Most of these functions below are messy but they are only used here and only for simplifying boilerplate // ha lol theyre all gone

    // Will I know what I did at 3:16 AM later? Hopefully. Right now this seems reasonable

    // older Zim says it's good enough and does the job. Because not used anywhere else, no real problems keeping this

    // even older Zim (18 yrs old now) says it turns out a lot of it got moved away anyways, so it worked perfectly as a temporary solution!

    // 19 now, and bam, its all gone to [LevelManager] or [LevelUtility]. 2:22 am mohican outdoor center

    override fun toString() = "${javaClass.simpleName}-id: $id, info: $info"
}

class NonexistentLevelException(message: String) : Exception(message)

class LevelSerializer<R : Level> : Serializer<R>() {

    override fun write(obj: Any, output: Output) {
        obj as Level
        output.write(obj.id)
    }

    override fun instantiate(input: Input): R {
        val id = input.read(UUID::class.java)
        val existingLevel = LevelManager.allLevels.firstOrNull { it.id == id }
                ?: if (id == LevelManager.EMPTY_LEVEL.id) LevelManager.EMPTY_LEVEL else null
        if (existingLevel == null) {
            throw NonexistentLevelException("Level with id $id does not exist yet and so cannot be deserialized")
        }
        return existingLevel as R
    }

    override fun read(newInstance: Any, input: Input) {
    }
}