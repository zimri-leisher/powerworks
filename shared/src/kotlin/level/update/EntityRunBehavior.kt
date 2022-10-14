package level.update

import behavior.BehaviorTree
import level.Level
import level.entity.Entity
import player.Player
import serialization.AsReference
import serialization.Id

class EntityRunBehavior(
    @Id(2)
    @AsReference
    val entity: Entity,
    @Id(3)
    val behaviorTree: BehaviorTree,
    @Id(4)
    val arg: Any?,
    level: Level
) : LevelUpdate(LevelUpdateType.ENTITY_RUN_BEHAVIOR, level) {
    override val playersToSendTo: Set<Player>?
        get() = null

    override fun canAct(): Boolean {
        return true
    }

    override fun act() {
        entity.behavior.run(behaviorTree, argument = arg)
    }

    override fun actGhost() {
    }

    override fun cancelActGhost() {
    }

    override fun equivalent(other: LevelUpdate): Boolean {
        if (other !is EntityRunBehavior) {
            return false
        }
        return other.entity === entity && other.behaviorTree === behaviorTree && other.arg == arg
    }
}