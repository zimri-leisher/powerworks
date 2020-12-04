package level.update

import behavior.BehaviorTree
import level.Level
import level.entity.Entity
import network.MovingObjectReference
import player.Player
import serialization.Id

/*
class EntityRunBehavior(
        @Id(3)
        val entityReference: MovingObjectReference,
        @Id(4)
val behavior: BehaviorTree,
        @Id(5)
val argument: Any?) : LevelUpdate(LevelUpdateType.ENTITY_RUN_BEHAVIOR) {
    override val playersToSendTo: Set<Player>?
        get() = null

    override fun canAct(level: Level): Boolean {
        if(entityReference.value == null) {
            return false
        }
        if(entityReference.value !is Entity) {
            return false
        }
        return true
    }

    override fun act(level: Level) {
        (entityReference.value as Entity).behavior.run(behavior, argument = argument)
    }

    override fun actGhost(level: Level) {
    }

    override fun cancelActGhost(level: Level) {
    }

    override fun equivalent(other: LevelUpdate): Boolean {

    }

    override fun resolveReferences() {
    }

}
 */