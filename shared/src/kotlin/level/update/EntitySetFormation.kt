package level.update

import level.Level
import level.LevelManager
import level.LevelPosition
import level.entity.Entity
import level.entity.Formation
import network.MovingObjectReference
import player.Player
import serialization.*

/**
 * A level update for setting the formation positions of a group of entities.
 */
class EntitySetFormation(
    /**
     * A map of references to entities and their positions in the formation.
     */
    @Id(2)
    @AsReference(true)
    val positions: Map<MovingObjectReference<out Entity>, LevelPosition>,
    /**
     * The center of the formation.
     */
    @Id(3)
    val center: LevelPosition, level: Level
) : LevelUpdate(LevelUpdateType.ENTITY_SET_FORMATION, level) {

    private constructor() : this(mapOf(), LevelPosition(0, 0, LevelManager.EMPTY_LEVEL), LevelManager.EMPTY_LEVEL)

    override val playersToSendTo: Set<Player>?
        get() = null

    override fun canAct(): Boolean {
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

    override fun act() {
        if (positions.isEmpty()) {
            return
        }
        val group = (positions.keys.first().value!! as Entity).group!!
        val formation = Formation(center, positions.mapKeys { it.key.value!! as Entity })
        group.formation = formation
    }

    override fun actGhost() {
    }

    override fun cancelActGhost() {
    }

    override fun equivalent(other: LevelUpdate): Boolean {
        if (other !is EntitySetFormation) {
            return false
        }

        if (other.positions.size != positions.size) {
            return false
        }
        if (other.positions.any { (key, value) ->
                key.value == null ||
                        positions.none { (key2, value2) ->
                            key.value !== key2.value && value != value2
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