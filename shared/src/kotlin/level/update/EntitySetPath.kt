package level.update

import behavior.leaves.EntityPath
import level.Level
import level.LevelManager
import level.LevelPosition
import level.entity.DefaultEntity
import level.entity.Entity
import level.entity.EntityType
import misc.Coord
import network.MovingObjectReference
import player.Player
import serialization.AsReference
import serialization.Id
import java.util.*

/**
 * A level update for setting the [EntityPath] of an [Entity].
 */
class EntitySetPath(
    /**
     * A reference to the [Entity] to set the path of.
     */
    @AsReference
    @Id(2) val entity: Entity,
    /**
     * The starting position of that entity.
     */
    @Id(4) val startPosition: Coord,
    /**
     * The path to give the [Entity].
     */
    @Id(3) val path: EntityPath, level: Level
) : LevelUpdate(LevelUpdateType.ENTITY_SET_PATH, level) {

    private constructor() : this(
        DefaultEntity(EntityType.ERROR, 0, 0),
        Coord(0, 0),
        EntityPath(LevelPosition(0, 0, LevelManager.EMPTY_LEVEL), listOf()), LevelManager.EMPTY_LEVEL
    )

    override val playersToSendTo: Set<Player>?
        get() = null

    override fun canAct(): Boolean {
        return true
    }

    override fun act() {
        entity.apply {
            setPosition(startPosition.x, startPosition.y)
            behavior.follow(path)
        }
    }

    override fun actGhost() {
    }

    override fun cancelActGhost() {
    }

    override fun equivalent(other: LevelUpdate): Boolean {
        if (other !is EntitySetPath) {
            return false
        }

        if (other.entity !== entity) {
            return false
        }
        return path == other.path && startPosition == other.startPosition
    }
}