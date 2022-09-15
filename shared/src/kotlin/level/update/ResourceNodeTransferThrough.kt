package level.update

import level.LevelManager
import network.ResourceNodeReference
import player.Player
import resource.ResourceList
import resource.emptyResourceList
import serialization.Id
import java.util.*

/**
 * A level update for transferring resources through a [ResourceNode].
 */
class ResourceNodeTransferThrough(
        /**
         * A reference to the [ResourceNode] to transfer resources through.
         */
        @Id(2) val nodeReference: ResourceNodeReference,
        /**
         * The resources to transfer through the node.
         */
        @Id(3) val resources: ResourceList,
        /**
         * If true, it will try to output the resources, if false, it will try to input them.
         */
        @Id(4) val output: Boolean,
        /**
         * If true, this will check if able first.
         */
        @Id(5) val checkIfAble: Boolean,
        /**
         * Whether or not to actually make sure that the resources can A) fit and B) exist in an attached container.
         */
        @Id(6) val mustContainOrHaveSpace: Boolean
) : GameUpdate(LevelUpdateType.RESOURCE_NODE_TRANSFER_THROUGH) {

    private constructor() : this(ResourceNodeReference(0, 0, LevelManager.EMPTY_LEVEL, UUID.randomUUID()), emptyResourceList(), false, false, false)

    override val playersToSendTo: Set<Player>?
        get() = null

    override fun canAct(): Boolean {
        if (nodeReference.value == null) {
            return false
        }
        val node = nodeReference.value!!
        if (output) {
            if (!node.canOutputAll(resources, mustContainOrHaveSpace)) {
                return false
            }
        } else {
            if (!node.canInput(resources, mustContainOrHaveSpace)) {
                return false
            }
        }
        return true
    }

    override fun act() {
        val node = nodeReference.value!!
        if (output) {
            node.output(resources, checkIfAble, mustContainOrHaveSpace)
        } else {
            node.input(resources, checkIfAble, mustContainOrHaveSpace)
        }
    }

    override fun actGhost() {
    }

    override fun cancelActGhost() {
    }

    override fun equivalent(other: GameUpdate): Boolean {
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