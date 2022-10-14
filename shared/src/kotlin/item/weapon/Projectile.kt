package item.weapon

import graphics.Image
import graphics.Renderer
import graphics.TextureRenderParams
import level.PhysicalLevelObject
import level.block.BlockType
import level.block.DefaultBlock
import level.entity.Entity
import level.getCollisionsWith
import misc.Geometry
import serialization.Id
import java.lang.Math.pow
import kotlin.math.*

// TODO why shouldn't these be physical level objects?
class Projectile(
        @Id(1)
        val type: ProjectileType,
        @Id(2)
        var x: Int,
        @Id(3)
        var y: Int,
        @Id(4)
        val angle: Float,
        @Id(5)
        val parent: PhysicalLevelObject) {

    private constructor() : this(ProjectileType.SMALL_BULLET, 0, 0, 0f, DefaultBlock(BlockType.ERROR, 0, 0))

    @Id(7)
    var xVel = 0f

    @Id(8)
    var yVel = 0f

    @Id(9)
    var xRemainder = 0f

    @Id(10)
    var yRemainder = 0f

    @Id(11)
    var ticksLived = 0

    @Id(12)
    val points: List<Pair<Float, Float>>

    @Id(13)
    val squareSideLength: Int

    init {
        // create verticies
        val points = listOf(
                -type.hitbox.width / 2 to -type.hitbox.height / 2,
                type.hitbox.width / 2 to -type.hitbox.height / 2,
                type.hitbox.width / 2 to type.hitbox.height / 2,
                -type.hitbox.width / 2 to type.hitbox.height / 2
        )
        // rotate vertices
        this.points = points.map {
            it.first * cos(angle) - it.second * sin(angle) to it.first * sin(angle) + it.second * cos(angle)
        }
        squareSideLength = ceil(sqrt(pow(type.hitbox.width.toDouble(), 2.0) + pow(type.hitbox.height.toDouble(), 2.0))).toInt()
    }

    fun render() {
        Renderer.renderTexture(Image.Weapon.PROJECTILE, x, y, type.hitbox.width, type.hitbox.height, TextureRenderParams(rotation = Math.toDegrees(angle.toDouble()).toFloat()))
    }

    fun onCollide(o: PhysicalLevelObject) {
        if (o is Entity && o.team == parent.team) {
            // ignore entity collisions if we're on the same team
            return
        }
        if (o.team != parent.team) {
            // only deal damage to enemy objects
            o.health -= type.damage
            if (o.health <= 0) {
                parent.level.remove(o)
            }
        }
        parent.level.remove(this)
    }

    fun update() {
        ticksLived++
        if (ticksLived >= type.lifetime) {
            parent.level.remove(this)
            return
        }
        xVel = type.speed * cos(angle)
        yVel = type.speed * sin(angle)
        xRemainder += xVel % 1
        yRemainder += yVel % 1
        x += xVel.toInt() + xRemainder.toInt()
        y += yVel.toInt() + yRemainder.toInt()
        xRemainder %= 1
        yRemainder %= 1
        val possibleCollisions = parent.level.getCollisionsWith(x, y, squareSideLength, squareSideLength)
        val actualCollision = getCollisionsWithRectangle(possibleCollisions, points.map { it.first + x + type.hitbox.width / 2 to it.second + y + type.hitbox.height / 2 }).firstOrNull()
        if (actualCollision != null) {
            onCollide(actualCollision)
        }
    }
}

private fun getCollisionsWithRectangle(possibleColliders: Sequence<PhysicalLevelObject>, points: List<Pair<Float, Float>>): Sequence<PhysicalLevelObject> {
    if (points.size != 4) {
        throw IllegalArgumentException("Rectangle must be defined by 4 points (had ${points.size})")
    }

    fun intersectsRectangle(levelObj: PhysicalLevelObject): Boolean {
        for (point in points) {
            if (Geometry.contains(levelObj.x + levelObj.hitbox.xStart, levelObj.y + levelObj.hitbox.yStart, levelObj.hitbox.width, levelObj.hitbox.height, point.first.toInt(), point.second.toInt(), 0, 0)) {
                return true
            }
        }
        return false
    }

    // find axis aligned bounds of rectangle
    var minX = 0
    var maxX = 0
    var minY = 0
    var maxY = 0
    for (point in points) {
        if (point.first < minX) {
            minX = floor(point.first).toInt()
        }
        if (point.first > maxX) {
            maxX = ceil(point.first).toInt()
        }
        if (point.second < minY) {
            minY = floor(point.second).toInt()
        }
        if (point.second > maxY) {
            maxY = ceil(point.second).toInt()
        }
    }
    return possibleColliders.filter { intersectsRectangle(it) }
}