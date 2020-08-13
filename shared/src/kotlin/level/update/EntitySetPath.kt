package level.update

import behavior.leaves.EntityPath
import level.Level
import level.LevelManager
import level.entity.Entity
import misc.PixelCoord
import network.MovingObjectReference
import player.Player
import serialization.Id
import java.util.*

class EntitySetPath(@Id(2) val entityReference: MovingObjectReference,
                    @Id(4) val startPosition: PixelCoord,
                    @Id(3) val path: EntityPath) : LevelUpdate(LevelUpdateType.ENTITY_SET_PATH) {

    private constructor() : this(MovingObjectReference(LevelManager.EMPTY_LEVEL, UUID.randomUUID(), 0, 0), PixelCoord(0, 0), EntityPath(PixelCoord(0, 0), listOf()))

    override val playersToSendTo: Set<Player>?
        get() = null

    override fun canAct(level: Level): Boolean {
        return entityReference.value != null
    }

    override fun act(level: Level) {
        (entityReference.value!! as Entity).apply {
            setPosition(startPosition.xPixel, startPosition.yPixel)
            behavior.follow(path)
        }
    }

    override fun actGhost(level: Level) {
    }

    override fun cancelActGhost(level: Level) {
    }

    override fun equivalent(other: LevelUpdate): Boolean {
        if (other !is EntitySetPath) {
            return false
        }

        if (other.entityReference.value == null || other.entityReference.value !== entityReference.value) {
            return false
        }
        return path == other.path && startPosition == other.startPosition
    }

    override fun resolveReferences() {
        entityReference.value = entityReference.resolve()
    }

}