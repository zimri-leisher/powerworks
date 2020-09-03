package level.entity

import data.WeakMutableList
import graphics.Renderer
import level.Level
import level.LevelManager
import level.canAdd
import level.update.EntitySetFormation
import misc.Geometry
import misc.PixelCoord
import network.MovingObjectReference
import serialization.Id
import java.awt.Rectangle

data class Formation(
        @Id(1)
        val center: PixelCoord,
        @Id(2)
        val positions: Map<Entity, PixelCoord>) {

    private constructor() : this(PixelCoord(0, 0), mapOf())

    @Id(3)
    val boundaries: Rectangle = Rectangle()

    init {
        for ((entity, position) in positions) {
            boundaries.add(Rectangle(
                    position.xPixel + entity.hitbox.xStart + entity.hitbox.width / 2,
                    position.yPixel + entity.hitbox.yStart + entity.hitbox.height / 2,
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

    val center: PixelCoord
        get() {
            val averageXPixel = entities.sumBy { it.xPixel } / entities.size
            val averageYPixel = entities.sumBy { it.yPixel } / entities.size
            return PixelCoord(averageXPixel, averageYPixel)
        }

    val inFormation: Boolean
        get() = formation != null &&
                formation!!.positions.all { (entity, pos) ->
                    Geometry.distance(entity.xPixel + entity.hitbox.xStart + entity.hitbox.width / 2, entity.yPixel + entity.hitbox.yStart + entity.hitbox.height / 2, pos.xPixel, pos.yPixel) < 8
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

    fun createFormationAround(xPixel: Int, yPixel: Int, padding: Int) {
        if (entities.isEmpty()) {
            return
        }
        if (formation != null && formation!!.center.xPixel == xPixel && formation!!.center.yPixel == yPixel) {
            // already created a formation around this center
            return
        }

        val biggestHitbox = entities.map { it.hitbox }.maxBy { it.width * it.height }!!

        val formationPositions = mutableListOf<PixelCoord>()

        // start by adding the first entity

        // spiral out from the location of the first entity
        var remainingEntityCount = entities.size

        var lastPosition = PixelCoord(xPixel, yPixel - padding)

        var currentDir = 0

        var currentLengthOfSpiral = 0
        var currentPosOnSpiral = 0
        while (remainingEntityCount > 0) {
            val nextPosition = PixelCoord(lastPosition.xPixel + padding * Geometry.getXSign(currentDir),
                    lastPosition.yPixel + padding * Geometry.getYSign(currentDir))
            if (level.canAdd(biggestHitbox, nextPosition.xPixel, nextPosition.yPixel)) {
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


        val newFormation = mutableMapOf<Entity, PixelCoord>()

        // what we're going to do to find out which entity should go to which point is:
        // recenter the formation around the center of the entity group
        // for each point, find the nearest entity and assign it to that
        val formationCenter = PixelCoord(formationPositions.sumBy { it.xPixel } / formationPositions.size, formationPositions.sumBy { it.yPixel } / formationPositions.size)
        val groupCenter = this.center

        val remainingEntities = mutableListOf<Entity>()
        remainingEntities.addAll(entities)

        formationPositions.forEach {
            val readjustedPoint = PixelCoord(it.xPixel - formationCenter.xPixel + groupCenter.xPixel, it.yPixel - formationCenter.yPixel + groupCenter.yPixel)
            val nearestEntity = remainingEntities.minBy { entity -> Geometry.distanceSq(readjustedPoint.xPixel, readjustedPoint.yPixel, entity.xPixel, entity.yPixel) }!!
            remainingEntities.remove(nearestEntity)
            newFormation[nearestEntity] = it
        }

        val level = entities.first().level
        level.modify(EntitySetFormation(newFormation.mapKeys { it.key.toReference() as MovingObjectReference }, PixelCoord(xPixel, yPixel)))
    }

    fun render() {
        if (formation == null) {
            return
        }
        for ((_, coord) in formation!!.positions) {
            Renderer.renderFilledRectangle(coord.xPixel, coord.yPixel, 1, 1)
        }
    }

    companion object {
        val ALL = WeakMutableList<EntityGroup>()
    }
}
