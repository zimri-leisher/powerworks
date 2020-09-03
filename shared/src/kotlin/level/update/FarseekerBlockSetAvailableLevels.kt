package level.update

import level.Level
import level.LevelInfo
import level.LevelManager
import level.block.FarseekerBlock
import network.BlockReference
import player.Player
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
        val farseekerReference: BlockReference,
        /**
         * A map of the UUID of the level and its [LevelInfo] to set as the available destination levels.
         */
        @Id(4)
        val levels: Map<UUID, LevelInfo>
) : LevelUpdate(LevelUpdateType.FARSEEKER_SET_AVAILABLE_LEVELS) {

    private constructor() : this(BlockReference(LevelManager.EMPTY_LEVEL, UUID.randomUUID(), 0, 0), mapOf())

    override val playersToSendTo: Set<Player>?
        get() = null

    override fun canAct(level: Level): Boolean {
        return farseekerReference.value != null && farseekerReference.value is FarseekerBlock
    }

    override fun act(level: Level) {
        val farseeker = farseekerReference.value!! as FarseekerBlock
        farseeker.availableDestinations = levels
    }

    override fun actGhost(level: Level) {
    }

    override fun cancelActGhost(level: Level) {
    }

    override fun equivalent(other: LevelUpdate): Boolean {
        if (other !is FarseekerBlockSetAvailableLevels) {
            return false
        }

        return other.farseekerReference.value != null && other.farseekerReference.value === farseekerReference.value && levels == other.levels
    }

    override fun resolveReferences() {
        farseekerReference.value = farseekerReference.resolve()
    }

}