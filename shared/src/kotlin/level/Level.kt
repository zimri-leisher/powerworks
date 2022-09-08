package level

import behavior.leaves.FindPath
import graphics.Renderer
import item.weapon.Projectile
import level.block.Block
import level.block.BlockType
import level.generator.EmptyLevelGenerator
import level.moving.MovingObject
import level.moving.MovingObjectType
import level.particle.Particle
import level.tile.Tile
import level.update.LevelObjectAdd
import level.update.LevelObjectRemove
import level.update.LevelUpdate
import main.DebugCode
import main.Game
import main.PowerworksDelegates
import network.ClientNetworkManager
import network.ServerNetworkManager
import resource.ResourceNetwork
import routing.ResourceRoutingNetwork
import screen.element.ElementLevelView
import screen.mouse.tool.Tool
import serialization.Input
import serialization.Output
import serialization.Serializer
import java.util.*

const val CHUNK_SIZE_TILES = 8
val CHUNK_TILE_EXP = (Math.log(CHUNK_SIZE_TILES.toDouble()) / Math.log(2.0)).toInt()
val CHUNK_EXP = CHUNK_TILE_EXP + 4
val CHUNK_SIZE = CHUNK_SIZE_TILES shl 4

/**
 * A level consists of the [Tile]s, [Block]s, [MovingObject]s and various other things that make up the actual gameplay.
 *
 * Loading, saving and other general tasks relating to [Level]s are done by the [LevelManager].
 *
 * [Level]s have various utility methods defined in the `LevelUtlity.kt` file as extension methods. These should be used
 * over direct access to chunks and their [LevelObject]s.
 *
 * [ActualLevel]s, which are usually on a central [ServerNetworkManager], represent the real, true data, whereas [RemoteLevel]s are only
 * copies for the [ClientNetworkManager] to have.
 *
 * Level instantiating is trivial and doesn't affect the game state. To load a level, first call [initialize], which adds
 * this level to various event handlers and listeners and actually adds it to [LevelManager.allLevels]. Then call [load],
 * which, depending on what kind of level this is, will either load it from the disk or request that the [LevelData] be sent
 * to it over the network.
 *
 * @param id the [UUID] of this level.
 * @param info the information describing the [Level]
 */
abstract class Level(
    val id: UUID,
    /**
     * The information describing this level. It defines the username of the owner of this level, the name of the level,
     * the generation settings and other related things (see [LevelInfo] for more detail). Two levels with the same [info] should be identically generated
     */
    val info: LevelInfo
) {

    val generator = info.levelType.getGenerator(this)

    var data: LevelData by PowerworksDelegates.lateinitVal()

    val widthChunks = info.levelType.widthChunks
    val heightChunks = info.levelType.heightChunks

    val widthTiles = widthChunks shl CHUNK_TILE_EXP
    val heightTiles = widthChunks shl CHUNK_TILE_EXP

    val height = heightChunks shl CHUNK_EXP
    val width = widthChunks shl CHUNK_EXP

    var loaded = false
        set(value) {
            if (field != value) {
                field = value
                if (generator !is EmptyLevelGenerator) {
                    if (field) {
                        LevelManager.loadedLevels.add(this)
                        LevelManager.pushLevelStateEvent(this, LevelEvent.LOAD)
                    } else if (!field) {
                        LevelManager.loadedLevels.remove(this)
                        LevelManager.pushLevelStateEvent(this, LevelEvent.UNLOAD)
                    }
                }
            }
        }

    var initialized = false
        set(value) {
            if (field != value) {
                field = value
                if (field && generator !is EmptyLevelGenerator) {
                    LevelManager.pushLevelStateEvent(this, LevelEvent.INITIALIZE)
                }
            }
        }

    var paused = false
        set(value) {
            if (field != value) {
                field = value
                LevelManager.pushLevelStateEvent(this, if (field) LevelEvent.PAUSE else LevelEvent.UNPAUSE)
            }
        }

    var updatesCount = 0

    open fun initialize() {
        if (generator !is EmptyLevelGenerator) {
            LevelManager.allLevels.forEach {
                if (it.id == id) {
                    throw IllegalArgumentException("A level with id $id already exists ($it)")
                }
            }
            LevelManager.allLevels.add(this)
        }
        initialized = true
    }

    open fun load() {
        loaded = true
    }

    open fun canModify(update: LevelUpdate) = loaded && update.canAct(this)

    open fun modify(update: LevelUpdate, transient: Boolean = false): Boolean {
        if (!canModify(update)) {
            return false
        }
        update.act(this)
        return true
    }

    /**
     * Does everything necessary to put the object to the level, if possible. If this is already in another
     * [Level], it will remove it from that level first.
     * @return true if the object was not already present and the object was added successfully
     */
    open fun add(l: LevelObject) = modify(LevelObjectAdd(l))

    /**
     * Does everything necessary to remove [l] from this level
     *
     * @return true if [l] was in this level before calling this method, and now is no longer
     */
    open fun remove(l: LevelObject) = modify(LevelObjectRemove(l))

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

//    /**
//     * Tries to add a resource node to the level. If [node] was already in another level before addition, it will remove it from
//     * that level first.
//     * If there was already a node at the same position with the same direction, attached container and resource
//     * category, it will remove the previous one before finishing addition.
//     *
//     * @return true if the node was added successfully (is now in this level)
//     */
//    open fun add(node: ResourceNode2): Boolean {
//        if (node.inLevel && node.level != this) {
//            node.level.remove(node)
//        }
//        val c = getChunkAtTile(node.xTile, node.yTile)
//        val previousNode = c.getResourceNode(node.xTile, node.yTile)
//        if (previousNode != null) {
//            remove(previousNode)
//        }
//        c.addResourceNode(node)
//        node.level = this
//        node.inLevel = true
//        return true
//    }

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

    open fun update() {
        ResourceRoutingNetwork.ALL.forEach { if (it.level == this) it.update() }
        ResourceNetwork.ALL.forEach { if(it.level == this) it.update() }
        data.projectiles.forEach { it.update() }
        data.chunks.forEach { it.update() }
        data.particles.forEach { it.update() }
        updatesCount++
    }

    fun render(view: ElementLevelView) {
        // Assume it is already added to views list
        val r = view.viewRectangle
        val x0 = r.minX.toInt()
        val y0 = r.minY.toInt()
        val x1 = r.maxX.toInt()
        val y1 = r.maxY.toInt()
        val xTile0 = x0 shr 4
        val yTile0 = y0 shr 4
        val xTile1 = (x1 shr 4) + 1
        val yTile1 = (y1 shr 4) + 1
        val maxX = xTile1.coerceAtMost(widthTiles)
        val maxY = yTile1.coerceAtMost(heightTiles)
        val minX = xTile0.coerceAtLeast(0)
        val minY = yTile0.coerceAtLeast(0)
        for (y in (maxY - 1) downTo minY) {
            for (x in minX until maxX) {
                val c = getChunkAtTile(x, y)
                c.getTile(x, y).render()
            }
        }
        Tool.renderBelow(this)
        data.particles.forEach {
            it.render()
        }
        val ghostBlocks = data.ghostObjects.filter { it.type is BlockType<*> }
        val ghostMovings = data.ghostObjects.filter { it.type is MovingObjectType<*> }
        for (y in (maxY - 1) downTo minY) {
            val yChunk = y shr CHUNK_TILE_EXP
            // Render the line of blocks
            for (x in minX until maxX) {
                val c = getChunkAtChunk(x shr CHUNK_TILE_EXP, yChunk)
                ghostBlocks.filter { it.xTile == x && it.yTile == y }.forEach { it.render() }
                c.getBlock(x, y)?.render()
            }
            // Render the moving objects in sorted order
            for (xChunk in (minX shr CHUNK_TILE_EXP)..(maxX shr CHUNK_TILE_EXP)) {
                val c = getChunkAtChunk(xChunk, yChunk)
                if (c.data.moving.isNotEmpty()) {
                    c.data.moving.forEach {
                        if (it.yTile >= y && it.yTile < y + 1) {
                            it.render()
                        }
                    }
                }
                ghostMovings.forEach {
                    if (it.yTile >= y && it.yTile < y + 1) {
                        it.render()
                    }
                }
            }
        }

        ResourceRoutingNetwork.ALL.forEach { if (it.level == this) it.render() }
        data.projectiles.forEach { it.render() }
        Tool.renderAbove(this)

        val chunksInTileRectangle = getChunksFromTileRectangle(minX, minY, maxX - minX - 1, maxY - minY - 1)
        for (chunk in chunksInTileRectangle) {
            for (node in chunk.data.resourceNodes) {
                node?.render()
            }
        }
        if (Game.currentDebugCode == DebugCode.CHUNK_INFO) {
            for (c in chunksInTileRectangle) {
                Renderer.renderEmptyRectangle(c.xTile shl 4, c.yTile shl 4, CHUNK_SIZE, CHUNK_SIZE)
                Renderer.renderText(
                    "x: ${c.xChunk}, y: ${c.yChunk}\n" +
                            "updates required: ${c.data.updatesRequired.size}\n" +
                            "moving objects: ${c.data.moving.size} (${c.data.movingOnBoundary.size} on boundary)",
                    c.xTile shl 4, c.yTile shl 4
                )
            }
        } else if (Game.currentDebugCode == DebugCode.PIPE_INFO) {
            ResourceNetwork.render()
        }

        FindPath.render()
    }

    // Most of these functions below are messy but they are only used here and only for simplifying boilerplate // ha lol theyre all gone

    // Will I know what I did at 3:16 AM later? Hopefully. Right now this seems reasonable

    // older Zim says it's good enough and does the job. Because not used anywhere else, no real problems keeping this

    // even older Zim (18 yrs old now) says it turns out a lot of it got moved away anyways, so it worked perfectly as a temporary solution!

    // 19 now, and bam, its all gone to [LevelManager] or [LevelUtility]. 2:22 am mohican outdoor center

    override fun toString() = "${javaClass.simpleName}-id: $id, info: $info"
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Level

        if (id != other.id) return false
        if (info != other.info) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + info.hashCode()
        return result
    }

}

class NonexistentLevelException(message: String) : Exception(message)

class LevelSerializer<R : Level> : Serializer<R>() {

    override fun write(obj: Any, output: Output) {
        obj as Level
        output.write(obj.id)
    }

    override fun instantiate(input: Input): R {
        val id = input.read(UUID::class.java)
        val existingLevel = LevelManager.getLevelByIdOrNull(id)
            ?: if (id == LevelManager.EMPTY_LEVEL.id) LevelManager.EMPTY_LEVEL else null
        if (existingLevel == null) {
            println("Unknown level $id")
            return UnknownLevel(id) as R
        }
        return existingLevel as R
    }

    override fun read(newInstance: Any, input: Input) {
    }
}