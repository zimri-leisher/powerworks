package level.entity.command

import behavior.Behavior
import level.LevelManager
import level.LevelPosition
import level.entity.Entity
import level.entity.EntityGroup
import serialization.Id

sealed class EntityCommand {
    abstract fun command(entity: Entity)
    open fun command(group: EntityGroup) {
        group.entities.forEach { command(it) }
    }
}

class MoveToCommand(
        @Id(1)
        val destination: LevelPosition
) : EntityCommand() {

    private constructor() : this(LevelPosition(0,0, LevelManager.EMPTY_LEVEL))

    override fun command(entity: Entity) {
        entity.behavior.goalPosition = destination
        entity.behavior.run(Behavior.Movement.PATH_TO_ARG, argument = destination)
    }

    override fun command(group: EntityGroup) {
        group.entities.forEach {
            it.behavior.goalPosition = destination
            it.behavior.run(Behavior.Movement.PATH_TO_FORMATION, argument = destination)
        }
    }
}