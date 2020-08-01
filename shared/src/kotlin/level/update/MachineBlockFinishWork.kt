package level.update

import level.Level
import level.LevelManager
import level.block.MachineBlock
import network.BlockReference
import player.Player
import serialization.Id
import java.util.*

class MachineBlockFinishWork(
        @Id(2) val blockReference: BlockReference
) : LevelUpdate(LevelModificationType.MACHINE_BLOCK_FINISH_WORK) {

    private constructor() : this(BlockReference(LevelManager.EMPTY_LEVEL, UUID.randomUUID(), 0, 0))

    override val playersToSendTo: Set<Player>?
        get() = null

    override fun canAct(level: Level) = blockReference.value != null && blockReference.value is MachineBlock

    override fun act(level: Level) {
        val block = blockReference.value!! as MachineBlock
        block.currentWork = 0
        block.onFinishWork()
    }

    override fun actGhost(level: Level) {
    }

    override fun cancelActGhost(level: Level) {
    }

    override fun equivalent(other: LevelUpdate): Boolean {
        if (other !is MachineBlockFinishWork) {
            return false
        }
        return other.blockReference.value == blockReference.value
    }

}