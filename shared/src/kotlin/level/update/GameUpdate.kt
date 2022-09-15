package level.update

import level.Level
import player.Player
import serialization.Id

enum class LevelUpdateType {
    DEFAULT,
    LEVEL_OBJECT_ADD,
    LEVEL_OBJECT_REMOVE,
    CRAFTER_SELECT_RECIPE,
    RESOURCE_NODE_TRANSFER_THROUGH,
    MACHINE_BLOCK_FINISH_WORK,
    ENTITY_SET_PATH,
    ENTITY_UPDATE_PATH_POSITION,
    ENTITY_ADD_TO_GROUP,
    ENTITY_SET_FORMATION,
    ENTITY_SET_TARGET,
    ENTITY_FIRE_WEAPON,
    FARSEEKER_SET_AVAILABLE_LEVELS,
    FARSEEKER_SET_DESTINATION_LEVEL,
    RESOURCE_NODE_BEHAVIOR_EDIT,
    LEVEL_OBJECT_SWITCH_LEVELS,
    ENTITY_RUN_BEHAVIOR,
    LEVEL_OBJECT_RESOURCE_CONTAINER_MODIFY,
    RESOURCE_TRANSACTION_EXECUTE
}

/**
 * This class generically represents something that can happen in a [Level]. These are the primary means for networked
 * communication of gameplay. To integrate a [GameUpdate] into the state of a [Level], use [Level.modify]. If the [Level] is an [ActualLevel]
 * and the update is able to be carried out, it will put that update into a [LevelUpdatePacket] and send it to the clients
 * defined in the [Level]'s [Lobby]. When it reaches the corresponding [RemoteLevel]s, it will be naively integrated.
 */
abstract class GameUpdate(
    @Id(1)
    val type: LevelUpdateType,
    @Id(-1)
    val level: Level
) {

    /**
     * A set of [Player]s to send this update to, or null to send to all players. The actual set of players sent to
     * is the intersection of this set and the players in the level's [Lobby].
     */
    abstract val playersToSendTo: Set<Player>?

    /**
     * @return whether or not this [GameUpdate] can act on the given [level].
     */
    abstract fun canAct(): Boolean

    /**
     * Integrates the update into the state of the [level] naively.
     */
    abstract fun act()

    /**
     * Graphically/sonically pretends to integrate the update into the state of the [level], but doesn't actually. Used
     * for instantaneous feedback in [RemoteLevel]s.
     */
    abstract fun actGhost()

    /**
     * Cancels the visual/audio effect taken in [actGhost].
     */
    abstract fun cancelActGhost()

    /**
     * @return whether or not the other modification is doing the same thing as this one. Useful for detecting if the server
     * and client are in sync.
     */
    abstract fun equivalent(other: GameUpdate): Boolean

    /**
     * Should re-resolve any [NetworkReference]s this [GameUpdate] stores. Necessary for when actions are initially unable to be taken due to
     * unresolved references but eventually become possible.
     */
    abstract fun resolveReferences()
}


class DefaultLevelUpdate : GameUpdate(LevelUpdateType.DEFAULT) {
    override val playersToSendTo: Set<Player>?
        get() = null

    override fun canAct() = false
    override fun act() {
    }

    override fun actGhost() {
    }

    override fun cancelActGhost() {
    }

    override fun equivalent(other: GameUpdate) = other is DefaultLevelUpdate

    override fun resolveReferences() {
    }
}