package level.update

import level.Level
import level.LevelInfo
import level.LevelManager
import level.block.FarseekerBlock
import network.BlockReference
import player.Player
import serialization.AsReference
import serialization.Id
import java.util.*

/**
 * A level update for setting the available destination [Level]s of a [FarseekerBlock].
 */
class FarseekerBlockSetAvailableLevels(
    /**
     * A reference to the [FarseekerBlock] to set the destination levels of.
     */
    @Id(3)
    @AsReference
    val farseeker: FarseekerBlock,
    /**
     * A map of the UUID of the level and its [LevelInfo] to set as the available destination levels.
     */
    @Id(4)
    val levels: Map<UUID, LevelInfo>, level: Level
) : LevelUpdate(LevelUpdateType.FARSEEKER_SET_AVAILABLE_LEVELS, level) {

    private constructor() : this(
        FarseekerBlock(0, 0),
        mapOf(),
        LevelManager.EMPTY_LEVEL
    )

    override val playersToSendTo: Set<Player>?
        get() = null

    override fun canAct(): Boolean {
        return true
    }

    override fun act() {
        farseeker.availableDestinations = levels
    }

    override fun actGhost() {
    }

    override fun cancelActGhost() {
    }

    override fun equivalent(other: LevelUpdate): Boolean {
        if (other !is FarseekerBlockSetAvailableLevels) {
            return false
        }

        return other.farseeker === farseeker && levels == other.levels
    }
}