package level.update

import level.entity.Entity
import level.entity.EntityGroup
import network.MovingObjectReference
import player.Player
import serialization.Id

/**
 * A level update for adding entities to an [EntityGroup].
 */
class EntityAddToGroup(
        /**
         * A list of references to entities to add to the group.
         */
        @Id(2) val entitiesInGroup: List<MovingObjectReference>) : GameUpdate(LevelUpdateType.ENTITY_ADD_TO_GROUP) {

    private constructor() : this(listOf())

    override val playersToSendTo: Set<Player>?
        get() = null

    override fun canAct(): Boolean {
        return entitiesInGroup.all { it.value != null }
    }

    override fun act() {
        val dereferencedEntities = entitiesInGroup.map { it.value!! as Entity }
        val group = EntityGroup(dereferencedEntities)
        dereferencedEntities.forEach { it.group = group }
    }

    override fun actGhost() {
    }

    override fun cancelActGhost() {
    }

    override fun equivalent(other: GameUpdate): Boolean {
        if (other !is EntityAddToGroup) {
            return false
        }

        if (other.entitiesInGroup.size != entitiesInGroup.size) {
            return false
        }

        if (other.entitiesInGroup.any { otherEntityRef -> otherEntityRef.value == null || entitiesInGroup.none { it.value === otherEntityRef.value } }) {
            return false
        }
        return true
    }

    override fun resolveReferences() {
        entitiesInGroup.forEach { it.value = it.resolve() }
    }

}