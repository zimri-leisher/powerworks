package level.update

import level.Level
import level.LevelManager
import level.block.BlockType
import level.block.DefaultBlock
import level.block.MachineBlock
import level.block.MinerBlock
import network.BlockReference
import player.Player
import serialization.AsReference
import serialization.Id
import java.util.*

/**
 * A level update for finishing a [MachineBlock]'s work cycle. Used to ensure factories are synchronized.
 */
class MachineBlockFinishWork(
    /**
     * A reference to the [MachineBlock] whose work is finished.
     */
    @Id(2)
    @AsReference
    val block: MachineBlock,
    level: Level
) : LevelUpdate(LevelUpdateType.MACHINE_BLOCK_FINISH_WORK, level) {

    private constructor() : this(
        MinerBlock(0, 0),
        LevelManager.EMPTY_LEVEL
    )

    override val playersToSendTo: Set<Player>?
        get() = null

    override fun canAct(): Boolean {
        return true
    }

    override fun act() {
        block.currentWork = 0
        block.onFinishWork()
    }

    override fun actGhost() {
    }

    override fun cancelActGhost() {
    }

    override fun equivalent(other: LevelUpdate): Boolean {
        if (other !is MachineBlockFinishWork) {
            return false
        }
        return other.block === block
    }
}