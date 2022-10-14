package level.update

import level.Level
import level.LevelManager
import level.PhysicalLevelObject
import level.block.BlockType
import level.block.DefaultBlock
import level.entity.DefaultEntity
import level.entity.Entity
import level.entity.EntityType
import level.moving.DefaultMovingObject
import level.moving.MovingObjectType
import network.BlockReference
import network.LevelObjectReference
import network.MovingObjectReference
import network.PhysicalLevelObjectReference
import player.Player
import serialization.AsReference
import serialization.Id
import java.util.*

/**
 * A level update for setting the shooting target of an [Entity]
 */
class EntitySetTarget(
    /**
     * A reference to the [Entity] to set the target of.
     */
    @AsReference
    @Id(2)
    val entity: Entity,
    /**
     * A reference to the target, or null if there is none.
     */
    @AsReference
    @Id(3)
    val target: PhysicalLevelObject?, level: Level

) : LevelUpdate(LevelUpdateType.ENTITY_SET_TARGET, level) {

    private constructor() : this(
        DefaultEntity(EntityType.ERROR, 0, 0),
        DefaultBlock(BlockType.ERROR, 0, 0),
        LevelManager.EMPTY_LEVEL
    )

    override val playersToSendTo: Set<Player>?
        get() = null

    override fun canAct(): Boolean {
        if (target != null) {
            return false
        }
        return true
    }

    override fun act() {
        entity.behavior.attackTarget = target
    }

    override fun actGhost() {
    }

    override fun cancelActGhost() {
    }

    override fun equivalent(other: LevelUpdate): Boolean {
        if (other !is EntitySetTarget) {
            return false
        }
        if (entity !== other.entity) {
            return false
        }
        if (target != other.target) {
            return false
        }
        return true
    }
}