package level.update

import level.Level
import level.LevelManager
import network.ResourceNodeReference
import player.Player
import resource.ResourceNodeBehavior
import serialization.Id
import java.util.*

/**
 * A level update for editing the [ResourceNodeBehavior] of [ResourceNode]s.
 */
class ResourceNodeBehaviorEdit(
    /**
     * A reference to the [ResourceNode] to set the behavior of.
     */
    @Id(3)
    val nodeReference: ResourceNodeReference,
    /**
     * The [ResourceNodeBehavior] to set the behavior to.
     */
    @Id(4)
    val behavior: ResourceNodeBehavior, level: Level

) : LevelUpdate(LevelUpdateType.RESOURCE_NODE_BEHAVIOR_EDIT, level) {

    private constructor() : this(
        ResourceNodeReference(0, 0, LevelManager.EMPTY_LEVEL, UUID.randomUUID()),
        ResourceNodeBehavior.EMPTY_BEHAVIOR, LevelManager.EMPTY_LEVEL
    )

    override val playersToSendTo: Set<Player>?
        get() = null

    override fun canAct(): Boolean {
        if (nodeReference.value == null) {
            return false
        }
        return true
    }

    override fun act() {
        nodeReference.value!!.behavior = behavior
    }

    override fun actGhost() {
    }

    override fun cancelActGhost() {
    }

    override fun equivalent(other: LevelUpdate): Boolean {
        if (other !is ResourceNodeBehaviorEdit) {
            return false
        }
        return other.nodeReference.value != null && other.nodeReference.value == nodeReference.value && other.behavior == behavior
    }

    override fun resolveReferences() {
        nodeReference.value = nodeReference.resolve()
    }

}