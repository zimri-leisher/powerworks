package level.update

import level.Level
import level.LevelManager
import level.block.FarseekerBlock
import network.BlockReference
import player.Player
import serialization.Id
import java.util.*

/**
 * A level update for setting the current destination level of a [FarseekerBlock].
 */
class FarseekerBlockSetDestinationLevel(
        /**
         * A reference to the [FarseekerBlock] to set the destination level of.
         */
        @Id(3)
        val blockReference: BlockReference,
        /**
         * The level to set the destination to.
         */
        @Id(4)
        val level: Level) : LevelUpdate(LevelUpdateType.FARSEEKER_SET_DESTINATION_LEVEL) {

    private constructor() : this(BlockReference(LevelManager.EMPTY_LEVEL, UUID.randomUUID(), 0, 0), LevelManager.EMPTY_LEVEL)

    override val playersToSendTo: Set<Player>?
        get() = null

    override fun canAct(level: Level): Boolean {
        if (blockReference.value == null) {
            println("reference null")
            return false
        }
        if (blockReference.value !is FarseekerBlock) {
            println("not farseeker")
            return false
        }
        if (this.level.id !in (blockReference.value as FarseekerBlock).availableDestinations.keys) {
            println("not available ${this.level.id}")
            println("available levels: ${(blockReference.value as FarseekerBlock).availableDestinations}")
            return false
        }
        return true
    }

    override fun act(level: Level) {
        val block = blockReference.value as FarseekerBlock
        block.destinationLevel = this.level
    }

    override fun actGhost(level: Level) {
    }

    override fun cancelActGhost(level: Level) {
    }

    override fun equivalent(other: LevelUpdate): Boolean {
        if (other !is FarseekerBlockSetDestinationLevel) {
            return false
        }

        if (other.blockReference.value == null) {
            return false
        }
        return other.blockReference.value == blockReference.value && other.level.id == level.id
    }

    override fun resolveReferences() {
        blockReference.value = blockReference.resolve()
    }

}