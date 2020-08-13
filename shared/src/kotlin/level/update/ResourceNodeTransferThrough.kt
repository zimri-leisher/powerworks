package level.update

import level.Level
import level.LevelManager
import network.ResourceNodeReference
import player.Player
import resource.ResourceList
import serialization.Id
import java.util.*

class ResourceNodeTransferThrough(
        @Id(2) val nodeReference: ResourceNodeReference,
        @Id(3) val resources: ResourceList,
        @Id(4) val output: Boolean,
        @Id(5) val checkIfAble: Boolean,
        @Id(6) val mustContainOrHaveSpace: Boolean
) : LevelUpdate(LevelUpdateType.RESOURCE_NODE_TRANSFER_THROUGH) {

    private constructor() : this(ResourceNodeReference(0, 0, LevelManager.EMPTY_LEVEL, UUID.randomUUID()), ResourceList(), false, false, false)

    override val playersToSendTo: Set<Player>?
        get() = null

    override fun canAct(level: Level): Boolean {
        if (nodeReference.value == null) {
            return false
        }
        val node = nodeReference.value!!
        if (output) {
            if (!node.canOutput(resources, mustContainOrHaveSpace)) {
                return false
            }
        } else {
            if (!node.canInput(resources, mustContainOrHaveSpace)) {
                return false
            }
        }
        return true
    }

    override fun act(level: Level) {
        val node = nodeReference.value!!
        if (output) {
            node.output(resources, checkIfAble, mustContainOrHaveSpace)
        } else {
            node.input(resources, checkIfAble, mustContainOrHaveSpace)
        }
    }

    override fun actGhost(level: Level) {
    }

    override fun cancelActGhost(level: Level) {
    }

    override fun equivalent(other: LevelUpdate): Boolean {
        if (other !is ResourceNodeTransferThrough) {
            return false
        }
        return other.nodeReference.value != null && other.nodeReference.value === nodeReference.value && other.resources == resources &&
                other.mustContainOrHaveSpace == mustContainOrHaveSpace && other.checkIfAble == checkIfAble
    }

    override fun resolveReferences() {
        nodeReference.value = nodeReference.resolve()
    }

}