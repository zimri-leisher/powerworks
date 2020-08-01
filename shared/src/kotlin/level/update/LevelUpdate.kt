package level.update

import level.Level
import player.Player
import serialization.Id

enum class LevelModificationType {
    DEFAULT,
    ADD_OBJECT,
    REMOVE_OBJECT,
    SELECT_CRAFTER_RECIPE,
    GIVE_BRAIN_ROBOT_ITEM,
    MODIFY_BLOCK_CONTAINER,
    TRANSFER_THROUGH_RESOURCE_NODE,
    MACHINE_BLOCK_FINISH_WORK,
    SET_ENTITY_PATH,
    UPDATE_ENTITY_PATH_POSITION,
    ADD_ENTITIES_TO_GROUP,
    SET_ENTITY_FORMATION,
    SET_ENTITY_TARGET,
    ENTITY_FIRE_WEAPON
}

abstract class LevelUpdate(
        @Id(1)
        val type: LevelModificationType) {

    abstract val playersToSendTo: Set<Player>?

    abstract fun canAct(level: Level): Boolean

    abstract fun act(level: Level)

    abstract fun actGhost(level: Level)
    abstract fun cancelActGhost(level: Level)

    abstract fun equivalent(other: LevelUpdate): Boolean
}


class DefaultLevelUpdate : LevelUpdate(LevelModificationType.DEFAULT) {
    override val playersToSendTo: Set<Player>?
        get() = null

    override fun canAct(level: Level) = false
    override fun act(level: Level) {
    }

    override fun actGhost(level: Level) {
    }

    override fun cancelActGhost(level: Level) {
    }

    override fun equivalent(other: LevelUpdate) = other is DefaultLevelUpdate
}