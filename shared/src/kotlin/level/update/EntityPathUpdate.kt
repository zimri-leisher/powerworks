package level.update

import level.Level
import level.LevelManager
import level.entity.Entity
import network.MovingObjectReference
import player.Player
import serialization.Id
import java.util.*

class EntityPathUpdate(@Id(2) val entityReference: MovingObjectReference,
                       @Id(5) val pathIndex: Int,
                       @Id(6) val timeReachedStep: Int,
                       @Id(4) val pathHash: Int) : LevelUpdate(LevelUpdateType.ENTITY_UPDATE_PATH_POSITION) {

    private constructor() : this(MovingObjectReference(LevelManager.EMPTY_LEVEL, UUID.randomUUID(), 0, 0), 0, 0, 0)

    override val playersToSendTo: Set<Player>?
        get() = null

    override fun canAct(level: Level) = entityReference.value != null

    override fun act(level: Level) {
        val entity = entityReference.value!! as Entity
        if (entity.behavior.path.hashCode() != pathHash) {
            println("path hash different, needs to resync")
        }
        (entityReference.value!! as Entity).behavior.shouldBeAtStepAtTime(pathIndex, timeReachedStep)
    }

    override fun actGhost(level: Level) {
    }

    override fun cancelActGhost(level: Level) {
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