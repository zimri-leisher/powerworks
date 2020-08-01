package level.update

import level.Level
import level.entity.Entity
import level.entity.Formation
import misc.PixelCoord
import network.MovingObjectReference
import player.Player
import serialization.Id

class EntitySetFormation(
        @Id(2)
        val positions: Map<MovingObjectReference, PixelCoord>,
        @Id(3)
        val center: PixelCoord
) : LevelUpdate(LevelModificationType.SET_ENTITY_FORMATION) {

    private constructor() : this(mapOf(), PixelCoord(0, 0))

    override val playersToSendTo: Set<Player>?
        get() = null

    override fun canAct(level: Level): Boolean {
        if (positions.isEmpty()) {
            return true
        }
        val entities = positions.keys.map { it.value as Entity? }
        if (entities.any { it == null }) {
            return false
        }
        entities as List<Entity>
        if (entities.map { it.group }.distinct().size != 1) {
            return false
        }
        return true
    }

    override fun act(level: Level) {
        val group = (positions.keys.first().value!! as Entity).group!!
        val formation = Formation(center, positions.mapKeys { it.key.value!! as Entity })
        group.formation = formation
    }

    override fun actGhost(level: Level) {
    }

    override fun cancelActGhost(level: Level) {
    }

    override fun equivalent(other: LevelUpdate): Boolean {
        if (other !is EntitySetFormation) {
            return false
        }

        if (other.positions.size != positions.size) {
            return false
        }
        if (other.positions.any { (key, value) ->
                    positions.none { (key2, value2) ->
                        key.value != key2.value && value != value2
                    }
                }) {
            return false
        }

        if (other.center != center) {
            return false
        }

        return true
    }

}