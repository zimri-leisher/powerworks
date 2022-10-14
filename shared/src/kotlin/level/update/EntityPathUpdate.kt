package level.update

import level.Level
import level.LevelManager
import level.entity.DefaultEntity
import level.entity.Entity
import level.entity.EntityType
import network.MovingObjectReference
import player.Player
import serialization.AsReference
import serialization.Id
import java.util.*

/**
 * A level update specifying the time and place an [Entity] reached a path step. Useful for ensuring synchronized movement between client and server.
 */
class EntityPathUpdate(
    /**
     * A reference to the [Entity] which reached the path step.
     */
    @Id(2)
    @AsReference
    val entity: Entity,
    /**
     * The index of that path step in the overall path.
     */
    @Id(5) val pathIndex: Int,
    /**
     * The time, in ticks since the [Level] started updating, that the entity reached that path step.
     */
    @Id(6) val timeReachedStep: Int,
    /**
     * The hash of the path. Useful for synchronization.
     */
    @Id(4) val pathHash: Int, level: Level
) : LevelUpdate(LevelUpdateType.ENTITY_UPDATE_PATH_POSITION, level) {

    private constructor() : this(
        DefaultEntity(EntityType.ERROR, 0, 0),
        0,
        0,
        0,
        LevelManager.EMPTY_LEVEL
    )

    override val playersToSendTo: Set<Player>?
        get() = null

    override fun canAct() = true

    override fun act() {
        if (entity.behavior.path.hashCode() != pathHash) {
            println("path hash different, needs to resync")
        }
        entity.behavior.shouldBeAtStepAtTime(pathIndex, timeReachedStep)
    }

    override fun actGhost() {
    }

    override fun cancelActGhost() {
    }

    override fun equivalent(other: LevelUpdate): Boolean {
        if (other !is EntityPathUpdate) {
            return false
        }

        return other.entity == entity && other.pathIndex == pathIndex && other.pathHash == pathHash && other.timeReachedStep == timeReachedStep
    }
}