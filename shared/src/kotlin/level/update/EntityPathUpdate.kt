package level.update

import level.Level
import level.LevelManager
import level.entity.Entity
import network.MovingObjectReference
import player.Player
import serialization.Id
import java.util.*

/**
 * A level update specifying the time and place an [Entity] reached a path step. Useful for ensuring synchronized movement between client and server.
 */
class EntityPathUpdate(
    /**
     * A reference to the [Entity] which reached the path step.
     */
    @Id(2) val entityReference: MovingObjectReference,
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
        MovingObjectReference(LevelManager.EMPTY_LEVEL, UUID.randomUUID(), 0, 0),
        0,
        0,
        0,
        LevelManager.EMPTY_LEVEL
    )

    override val playersToSendTo: Set<Player>?
        get() = null

    override fun canAct() = entityReference.value != null

    override fun act() {
        val entity = entityReference.value!! as Entity
        if (entity.behavior.path.hashCode() != pathHash) {
            println("path hash different, needs to resync")
        }
        (entityReference.value!! as Entity).behavior.shouldBeAtStepAtTime(pathIndex, timeReachedStep)
    }

    override fun actGhost() {
    }

    override fun cancelActGhost() {
    }

    override fun equivalent(other: LevelUpdate): Boolean {
        if (other !is EntityPathUpdate) {
            return false
        }

        return other.entityReference.value != null && other.entityReference.value == entityReference.value && other.pathIndex == pathIndex && other.pathHash == pathHash && other.timeReachedStep == timeReachedStep
    }

    override fun resolveReferences() {
        entityReference.value = entityReference.resolve()
    }
}