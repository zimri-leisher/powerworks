package level.update

import level.Level
import level.LevelManager
import level.entity.Entity
import network.BlockReference
import network.LevelObjectReference
import network.MovingObjectReference
import player.Player
import serialization.Id
import java.util.*

class EntitySetTarget(
        @Id(2)
        val entityReference: MovingObjectReference,
        @Id(3)
        val target: LevelObjectReference?
) : LevelUpdate(LevelModificationType.SET_ENTITY_TARGET) {

    private constructor() : this(MovingObjectReference(LevelManager.EMPTY_LEVEL, UUID.randomUUID(), 0, 0), BlockReference(LevelManager.EMPTY_LEVEL, UUID.randomUUID(), 0, 0))

    override val playersToSendTo: Set<Player>?
        get() = null

    override fun canAct(level: Level): Boolean {
        if (entityReference.value == null) {
            return false
        }
        if (target != null && target.value == null) {
            return false
        }
        return true
    }

    override fun act(level: Level) {
        (entityReference.value!! as Entity).behavior.target = target?.value
    }

    override fun actGhost(level: Level) {
    }

    override fun cancelActGhost(level: Level) {
    }

    override fun equivalent(other: LevelUpdate): Boolean {
        if (other !is EntitySetTarget) {
            return false
        }
        if (entityReference.value != null && other.entityReference.value != entityReference.value) {
            return false
        }
        if (target?.value != other.target?.value) {
            return false
        }
        return true
    }

}