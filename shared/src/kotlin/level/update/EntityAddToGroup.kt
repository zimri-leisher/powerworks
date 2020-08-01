package level.update

import level.Level
import level.entity.Entity
import level.entity.EntityGroup
import network.MovingObjectReference
import player.Player
import serialization.Id

class EntityAddToGroup(@Id(2) val entitiesInGroup: List<MovingObjectReference>) : LevelUpdate(LevelModificationType.ADD_ENTITIES_TO_GROUP) {

    private constructor() : this(listOf())

    override val playersToSendTo: Set<Player>?
        get() = null

    override fun canAct(level: Level): Boolean {
        return entitiesInGroup.all { it.value != null }
    }

    override fun act(level: Level) {
        val dereferencedEntities = entitiesInGroup.map { it.value!! as Entity }
        val group = EntityGroup(dereferencedEntities)
        dereferencedEntities.forEach { it.group = group }
    }

    override fun actGhost(level: Level) {
    }

    override fun cancelActGhost(level: Level) {
    }

    override fun equivalent(other: LevelUpdate): Boolean {
        if (other !is EntityAddToGroup) {
            return false
        }

        if (other.entitiesInGroup.size != entitiesInGroup.size) {
            return false
        }

        if (other.entitiesInGroup.any { otherEntityRef -> entitiesInGroup.none { it.value == otherEntityRef.value } }) {
            return false
        }
        return true
    }

}