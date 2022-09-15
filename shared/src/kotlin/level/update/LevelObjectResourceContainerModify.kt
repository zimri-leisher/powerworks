package level.update

import level.GhostLevelObject
import level.LevelObjectType
import level.entity.robot.BrainRobot
import network.GhostLevelObjectReference
import network.LevelObjectReference
import player.Player
import resource.ResourceList
import resource.emptyResourceList
import serialization.Id
import java.util.*

class LevelObjectResourceContainerModify(
        @Id(2)
        val levelObjectReference: LevelObjectReference,
        @Id(5)
        val containerId: UUID,
        @Id(3)
        val add: Boolean,
        @Id(4)
        val resources: ResourceList) : GameUpdate(LevelUpdateType.LEVEL_OBJECT_RESOURCE_CONTAINER_MODIFY) {
    constructor(brainRobot: BrainRobot, add: Boolean, resources: ResourceList) : this(brainRobot.toReference(), brainRobot.inventory.id, add, resources)
    private constructor() : this(GhostLevelObjectReference(GhostLevelObject(LevelObjectType.ERROR, 0, 0, 0)), UUID.randomUUID(), false, emptyResourceList())

    override val playersToSendTo: Set<Player>?
        get() = if(levelObjectReference.value == null) null else levelObjectReference.value!!.team.players

    override fun canAct(): Boolean {
        if (levelObjectReference.value == null) {
            return false
        }
        val container = levelObjectReference.value!!.containers.firstOrNull { it.id == containerId } ?: return false
        return if (add) container.canAdd(resources) else container.canRemove(resources)
    }

    override fun act() {
        val container = levelObjectReference.value!!.containers.first { it.id == containerId }
        if (add) {
            container.add(resources)
        } else {
            container.remove(resources)
        }
    }

    override fun actGhost() {
    }

    override fun cancelActGhost() {
    }

    override fun equivalent(other: GameUpdate): Boolean {
        if (other !is LevelObjectResourceContainerModify) {
            return false
        }
        if (other.levelObjectReference.value == null || other.levelObjectReference.value !== levelObjectReference.value) {
            return false
        }
        if (other.resources != resources || other.containerId != containerId) {
            return false
        }
        return true
    }

    override fun resolveReferences() {
    }

}