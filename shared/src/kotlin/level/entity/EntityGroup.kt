package level.entity

import data.WeakMutableList
import graphics.Renderer
import level.Level
import level.LevelManager
import level.canAdd
import level.update.EntitySetFormation
import misc.Geometry
import misc.Coord
import network.MovingObjectReference
import serialization.Id
import java.awt.Rectangle

data class Formation(
        @Id(1)
        val center: Coord,
        @Id(2)
        val positions: Map<Entity, Coord>) {

    private constructor() : this(Coord(0, 0), mapOf())

    @Id(3)
    val boundaries: Rectangle = Rectangle()

    init {
        for ((entity, position) in positions) {
            boundaries.add(Rectangle(
                    position.x + entity.hitbox.xStart + entity.hitbox.width / 2,
                    position.y + entity.hitbox.yStart + entity.hitbox.height / 2,
                    entity.hitbox.width,
                    entity.hitbox.height))
        }
    }
}

/**
 * A mutable group of [Entity]s. Used for commanding multiple entities at once instead of just one. This is where the
 * group [Formation] is stored.
 */
class EntityGroup(
        entities: List<Entity> = listOf()) {

    @Id(1)
    private val mutableEntities = mutableListOf<Entity>()

    val entities: List<Entity>
        get() = mutableEntities

    @Id(2)
    var level: Level = LevelManager.EMPTY_LEVEL

    init {
        entities.forEach { add(it) }
    }

    @Id(3)
    var formation: Formation? = null

    val center: Coord
        get() {
            val averageX = entities.sumBy { it.x } / entities.size
            val averageY = entities.sumBy { it.y } / entities.size
            return Coord(averageX, averageY)
        }

    val inFormation: Boolean
        get() = formation != null &&
                formation!!.positions.all { (entity, pos) ->
                    Geometry.distance(entity.x + entity.hitbox.xStart + entity.hitbox.width / 2, entity.y + entity.hitbox.yStart + entity.hitbox.height / 2, pos.x, pos.y) < 8
                }

    init {
        ALL.add(this)
    }

    fun add(entity: Entity) {
        if (level == LevelManager.EMPTY_LEVEL) {
            level = entity.level
        } else if (entity.level != level) {
            throw IllegalArgumentException("Tried to add an entity to entity group that had a different level than the group")
        }
        entity.group = this
        mutableEntities.add(entity)
    }

    fun remove(entity: Entity) {
        entity.group = null
        mutableEntities.remove(entity)
    }

    fun createFormationAround(x: Int, y: Int, padding: Int) {
        if (entities.isEmpty()) {
            return
        }
        if (formation != null && formation!!.center.x == x && formation!!.center.y == y) {
            // already created a formation around this center
            return
        }

        val biggestHitbox = entities.map { it.hitbox }.maxBy { it.width * it.height }!!

        val formationPositions = mutableListOf<Coord>()

        // start by adding the first entity

        // spiral out from the location of the first entity
        var remainingEntityCount = entities.size

        var lastPosition = Coord(x, y - padding)

        var currentDir = 0

        var currentLengthOfSpiral = 0
        var currentPosOnSpiral = 0
        while (remainingEntityCount > 0) {
            val nextPosition = Coord(lastPosition.x + padding * Geometry.getXSign(currentDir),
                    lastPosition.y + padding * Geometry.getYSign(currentDir))
            if (level.canAdd(biggestHitbox, nextPosition.x, nextPosition.y)) {
                formationPositions.add(nextPosition)
                remainingEntityCount--
            }
            if (currentPosOnSpiral == currentLengthOfSpiral) {
                // the length of each arm of the spiral increases by one every two sides
                if (currentDir == 0 || currentDir == 2) {
                    currentLengthOfSpiral++
                }
                currentPosOnSpiral = 0
                currentDir = Geometry.addAngles(currentDir, 1)
            }
            currentPosOnSpiral++
            lastPosition = nextPosition
        }


        val newFormation = mutableMapOf<Entity, Coord>()

        // what we're going to do to find out which entity should go to which point is:
        // recenter the formation around the center of the entity group
        // for each point, find the nearest entity and assign it to that
        val formationCenter = Coord(formationPositions.sumBy { it.x } / formationPositions.size, formationPositions.sumBy { it.y } / formationPositions.size)
        val groupCenter = this.center

        val remainingEntities = mutableListOf<Entity>()
        remainingEntities.addAll(entities)

        formationPositions.forEach {
            val readjustedPoint = Coord(it.x - formationCenter.x + groupCenter.x, it.y - formationCenter.y + groupCenter.y)
            val nearestEntity = remainingEntities.minBy { entity -> Geometry.distanceSq(readjustedPoint.x, readjustedPoint.y, entity.x, entity.y) }!!
            remainingEntities.remove(nearestEntity)
            newFormation[nearestEntity] = it
        }

        val level = entities.first().level
        level.modify(EntitySetFormation(newFormation.mapKeys { it.key.toReference() as MovingObjectReference }, Coord(x, y)))
    }

    fun render() {
        if (formation == null) {
            return
        }
        for ((_, coord) in formation!!.positions) {
            Renderer.renderFilledRectangle(coord.x, coord.y, 1, 1)
        }
    }

    companion object {
        val ALL = WeakMutableList<EntityGroup>()
    }
}
