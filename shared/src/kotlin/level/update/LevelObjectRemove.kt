package level.update

import level.*
import level.block.Block
import level.block.BlockType
import level.block.DefaultBlock
import level.entity.robot.BrainRobot
import level.moving.MovingObject
import network.BlockReference
import network.LevelObjectReference
import player.Player
import resource.ResourceContainer
import resource.ResourceNetwork
import resource.ResourceNode
import serialization.AsReference
import serialization.Id
import serialization.TryToResolveReferences
import java.util.*

/**
 * A level update for removing a [LevelObject] from a level.
 */
@TryToResolveReferences
class LevelObjectRemove(
    /**
     * A reference to the [LevelObject] to remove.
     */
    @AsReference
    @Id(2)
    val obj: LevelObject,
    level: Level
) : LevelUpdate(LevelUpdateType.LEVEL_OBJECT_REMOVE, level) {

    private constructor() : this(DefaultBlock(BlockType.ERROR,0,0), LevelManager.EMPTY_LEVEL)

    constructor(obj: LevelObject) : this(obj, obj.level)

    override val playersToSendTo: Set<Player>?
        get() = null

    override fun canAct() = obj.level == level

    override fun act() {
        if(obj is PhysicalLevelObject) {
            if (obj is Block) {
                for (x in 0 until obj.type.widthTiles) {
                    for (y in 0 until obj.type.heightTiles) {
                        level.getChunkAtTile(obj.xTile + x, obj.yTile + y)
                            .removeBlock(obj, obj.xTile + x, obj.yTile + y, (x == 0 && y == 0))
                    }
                }
                obj.inLevel = false
            } else if (obj is MovingObject) {
                val chunk = level.getChunkAtChunk(obj.xChunk, obj.yChunk)
                if (obj.hitbox != Hitbox.NONE)
                    obj.intersectingChunks.forEach { it.data.movingOnBoundary.remove(obj) }
                chunk.removeMoving(obj)
                obj.inLevel = false
                if (obj is BrainRobot) {
                    level.data.brainRobots.remove(obj)
                }
            } else if (obj is GhostLevelObject) {
                level.data.ghostObjects.remove(obj)
                obj.inLevel = false
                obj.level = LevelManager.EMPTY_LEVEL
            } else if (obj is ResourceNode) {
                level.getChunkAtTile(obj.xTile, obj.yTile).removeResourceNode(obj)
                obj.inLevel = false
            }
        } else {
            if(obj is ResourceContainer) {
                level.data.resourceContainers.remove(obj)
            } else if(obj is ResourceNetwork<*>) {
                level.data.resourceNetworks.remove(obj)
            }
            obj.inLevel = false
        }
    }

    override fun actGhost() {
        if (obj is GhostLevelObject) {
            // ghost act on a ghost object is real
            level.data.ghostObjects.remove(obj)
            obj.inLevel = false
            obj.level = LevelManager.EMPTY_LEVEL
        } else {
            // TODO hide it
        }
    }

    override fun cancelActGhost() {
        // TODO unhide it
    }

    override fun equivalent(other: LevelUpdate): Boolean {
        if (other !is LevelObjectRemove) {
            return false
        }

        if (obj !== other.obj) {
            return false
        }

        return true
    }

    override fun toString(): String {
        return "LevelObjectRemove(obj=$obj, level=$level)"
    }
}