package level.update

import level.*
import level.block.Block
import level.entity.robot.BrainRobot
import level.moving.MovingObject
import network.BlockReference
import network.LevelObjectReference
import player.Player
import serialization.Id
import java.util.*

/**
 * A level update for removing a [LevelObject] from a level.
 */
class LevelObjectRemove(
        /**
         * A reference to the [LevelObject] to remove.
         */
        @Id(2)
        val objReference: LevelObjectReference
) : LevelUpdate(LevelUpdateType.LEVEL_OBJECT_REMOVE) {

    private constructor() : this(BlockReference(LevelManager.EMPTY_LEVEL, UUID.randomUUID(), 0, 0))

    constructor(obj: LevelObject) : this(obj.toReference())

    override val playersToSendTo: Set<Player>?
        get() = null

    override fun canAct(level: Level) = objReference.value != null && objReference.level == level

    override fun act(level: Level) {
        val obj = objReference.value!!
        if (obj is Block) {
            for (x in 0 until obj.type.widthTiles) {
                for (y in 0 until obj.type.heightTiles) {
                    level.getChunkAtTile(obj.xTile + x, obj.yTile + y).removeBlock(obj, obj.xTile + x, obj.yTile + y, (x == 0 && y == 0))
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
        }
    }

    override fun actGhost(level: Level) {
        val obj = objReference.value!!
        if (obj is GhostLevelObject) {
            // ghost act on a ghost object is real
            level.data.ghostObjects.remove(obj)
            obj.inLevel = false
            obj.level = LevelManager.EMPTY_LEVEL
        } else {
            // TODO hide it
        }
    }

    override fun cancelActGhost(level: Level) {
        // TODO unhide it
    }

    override fun equivalent(other: LevelUpdate): Boolean {
        if (other !is LevelObjectRemove) {
            return false
        }

        if (objReference.value == null || objReference.value !== other.objReference.value) {
            return false
        }

        return true
    }

    override fun resolveReferences() {
        objReference.value = objReference.resolve()
    }

}