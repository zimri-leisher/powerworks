package level.update

import level.Level
import level.LevelManager
import level.block.Block
import network.BlockReference
import player.Player
import resource.ResourceList
import serialization.Id
import java.util.*

class BlockContainerModify(
        @Id(2) val blockReference: BlockReference,
        @Id(5) val containerId: UUID,
        @Id(3) val resources: ResourceList,
        @Id(4) val add: Boolean
) : LevelUpdate(LevelModificationType.MODIFY_BLOCK_CONTAINER) {

    private constructor() : this(BlockReference(LevelManager.EMPTY_LEVEL, UUID.randomUUID(), 0, 0), UUID.randomUUID(), ResourceList(), false)

    override val playersToSendTo: Set<Player>?
        get() = null

    override fun canAct(level: Level): Boolean {
        if (blockReference.value == null || blockReference.value !is Block) {
            return false
        }
        val block = blockReference.value as Block
        val container = block.containers.firstOrNull { it.id == containerId } ?: return false

        if (!add) {
            if (!container.canRemove(resources)) {
                return false
            }
        } else {
            if (!container.canAdd(resources)) {
                return false
            }
        }
        return true
    }

    override fun act(level: Level) {
        val block = blockReference.value as Block
        val container = block.containers.first { it.id == containerId }
        if (!add) {
            container.remove(resources)
        } else {
            container.add(resources)
        }
    }

    override fun actGhost(level: Level) {
        // TODO add ghost items to container
    }

    override fun cancelActGhost(level: Level) {
        // TODO add ghost items to container
    }

    override fun equivalent(other: LevelUpdate): Boolean {
        if (other !is BlockContainerModify) {
            return false
        }
        if (other.blockReference.value != blockReference.value) {
            return false
        }
        if (other.resources != resources || other.containerId != containerId) {
            return false
        }
        return true
    }

}