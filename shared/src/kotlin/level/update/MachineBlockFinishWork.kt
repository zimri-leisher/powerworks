package level.update

import level.LevelManager
import level.block.MachineBlock
import network.BlockReference
import player.Player
import serialization.Id
import java.util.*

/**
 * A level update for finishing a [MachineBlock]'s work cycle. Used to ensure factories are synchronized.
 */
class MachineBlockFinishWork(
        /**
         * A reference to the [MachineBlock] whose work is finished.
         */
        @Id(2) val blockReference: BlockReference
) : GameUpdate(LevelUpdateType.MACHINE_BLOCK_FINISH_WORK) {

    private constructor() : this(BlockReference(LevelManager.EMPTY_LEVEL, UUID.randomUUID(), 0, 0))

    override val playersToSendTo: Set<Player>?
        get() = null

    override fun canAct(): Boolean {
        if(blockReference.value == null) {
            return false
        }
        if(blockReference.value !is MachineBlock) {
            return false
        }
        return true
    }

    override fun act() {
        val block = blockReference.value!! as MachineBlock
        block.currentWork = 0
        block.onFinishWork()
    }

    override fun actGhost() {
    }

    override fun cancelActGhost() {
    }

    override fun equivalent(other: GameUpdate): Boolean {
        if (other !is MachineBlockFinishWork) {
            return false
        }
        return other.blockReference.value != null && other.blockReference.value === blockReference.value
    }

    override fun resolveReferences() {
        blockReference.value = blockReference.resolve()
    }

}