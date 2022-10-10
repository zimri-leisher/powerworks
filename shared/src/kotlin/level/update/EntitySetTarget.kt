package level.update

import level.Level
import level.LevelManager
import level.PhysicalLevelObject
import level.entity.Entity
import network.BlockReference
import network.LevelObjectReference
import network.MovingObjectReference
import network.PhysicalLevelObjectReference
import player.Player
import serialization.Id
import java.util.*

/**
 * A level update for setting the shooting target of an [Entity]
 */
class EntitySetTarget(
    /**
     * A reference to the [Entity] to set the target of.
     */
    @Id(2)
    val entityReference: MovingObjectReference,
    /**
     * A reference to the target, or null if there is none.
     */
    @Id(3)
    val target: PhysicalLevelObjectReference?, level: Level

) : LevelUpdate(LevelUpdateType.ENTITY_SET_TARGET, level) {

    private constructor() : this(
        MovingObjectReference(LevelManager.EMPTY_LEVEL, UUID.randomUUID(), 0, 0),
        BlockReference(LevelManager.EMPTY_LEVEL, UUID.randomUUID(), 0, 0), LevelManager.EMPTY_LEVEL
    )

    override val playersToSendTo: Set<Player>?
        get() = null

    override fun canAct(): Boolean {
        if (entityReference.value == null) {
            return false
        }
        if (target != null && target.value == null) {
            return false
        }
        return true
    }

    override fun act() {
        (entityReference.value!! as Entity).behavior.attackTarget = target?.value as PhysicalLevelObject
    }

    override fun actGhost() {
    }

    override fun cancelActGhost() {
    }

    override fun equivalent(other: LevelUpdate): Boolean {
        if (other !is EntitySetTarget) {
            return false
        }
        if (entityReference.value == null || other.entityReference.value !== entityReference.value) {
            return false
        }
        if (target?.value != other.target?.value) {
            return false
        }
        return true
    }

    override fun resolveReferences() {
        entityReference.value = entityReference.resolve()
    }

}