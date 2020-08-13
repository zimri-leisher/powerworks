package level.update

import level.Level
import player.Player
import serialization.Id

enum class LevelUpdateType {
    DEFAULT,
    LEVEL_OBJECT_ADD,
    LEVEL_OBJECT_REMOVE,
    CRAFTER_SELECT_RECIPE,
    BRAIN_ROBOT_GIVE_ITEM,
    BLOCK_CONTAINER_MODIFY,
    RESOURCE_NODE_TRANSFER_THROUGH,
    MACHINE_BLOCK_FINISH_WORK,
    ENTITY_SET_PATH,
    ENTITY_UPDATE_PATH_POSITION,
    ENTITY_ADD_TO_GROUP,
    ENTITY_SET_FORMATION,
    ENTITY_SET_TARGET,
    ENTITY_FIRE_WEAPON,
    FARSEEKER_SET_AVAILABLE_LEVELS,
    RESOURCE_NODE_BEHAVIOR_EDIT
}

abstract class LevelUpdate(
        @Id(1)
        val type: LevelUpdateType) {

    abstract val playersToSendTo: Set<Player>?

    abstract fun canAct(level: Level): Boolean

    abstract fun act(level: Level)

    abstract fun actGhost(level: Level)
    abstract fun cancelActGhost(level: Level)

    abstract fun equivalent(other: LevelUpdate): Boolean

    abstract fun resolveReferences()
}


class DefaultLevelUpdate : LevelUpdate(LevelUpdateType.DEFAULT) {
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

    override fun resolveReferences() {
    }
}