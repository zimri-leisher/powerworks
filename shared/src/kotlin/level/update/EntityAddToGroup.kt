package level.update

import level.Level
import level.LevelManager
import level.entity.Entity
import level.entity.EntityGroup
import level.moving.MovingObject
import network.MovingObjectReference
import player.Player
import serialization.AsReference
import serialization.Id

/**
 * A level update for adding entities to an [EntityGroup].
 */
class EntityAddToGroup(
    /**
     * A list of references to entities to add to the group.
     */
    @Id(2)
    @AsReference(true)
    val entitiesInGroup: List<Entity>,
    level: Level
) : LevelUpdate(LevelUpdateType.ENTITY_ADD_TO_GROUP, level) {

    private constructor() : this(listOf(), LevelManager.EMPTY_LEVEL)

    override val playersToSendTo: Set<Player>?
        get() = null

    override fun canAct(): Boolean {
        return true
    }

    override fun act() {
        val group = EntityGroup(entitiesInGroup)
        entitiesInGroup.forEach { it.group = group }
    }

    override fun actGhost() {
    }

    override fun cancelActGhost() {
    }

    override fun equivalent(other: LevelUpdate): Boolean {
        if (other !is EntityAddToGroup) {
            return false
        }

        if (other.entitiesInGroup.size != entitiesInGroup.size) {
            return false
        }

        if (other.entitiesInGroup.any { otherEntity ->  entitiesInGroup.none { it === otherEntity } }) {
            return false
        }
        return true
    }
}