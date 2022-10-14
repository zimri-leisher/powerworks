package level.update

import level.Level
import level.LevelManager
import level.block.FarseekerBlock
import network.BlockReference
import player.Player
import serialization.AsReference
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
    @AsReference
    val farseeker: FarseekerBlock,
    /**
     * The level to set the destination to.
     */
    @Id(4)
    val destinationLevel: Level, level: Level
) : LevelUpdate(LevelUpdateType.FARSEEKER_SET_DESTINATION_LEVEL, level) {

    private constructor() : this(
        FarseekerBlock(0, 0),
        LevelManager.EMPTY_LEVEL,
        LevelManager.EMPTY_LEVEL
    )

    override val playersToSendTo: Set<Player>?
        get() = null

    override fun canAct(): Boolean {
        if (this.level.id !in farseeker.availableDestinations.keys) {
            println("not available ${this.level.id}")
            println("available levels: ${farseeker.availableDestinations}")
            return false
        }
        return true
    }

    override fun act() {
        farseeker.destinationLevel = destinationLevel
    }

    override fun actGhost() {
    }

    override fun cancelActGhost() {
    }

    override fun equivalent(other: LevelUpdate): Boolean {
        if (other !is FarseekerBlockSetDestinationLevel) {
            return false
        }

        return other.farseeker === farseeker && other.level.id == level.id
    }
}