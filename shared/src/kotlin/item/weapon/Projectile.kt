package item.weapon

import graphics.Renderer
import graphics.TextureRenderParams
import level.LevelObject
import level.entity.Entity
import level.getCollisionsWithPoint
import serialization.Id
import java.awt.Point
import java.awt.Polygon
import kotlin.math.cos
import kotlin.math.sin

class Projectile(
        @Id(1)
        val type: ProjectileType,
        @Id(2)
        var xPixel: Int,
        @Id(3)
        var yPixel: Int,
        @Id(4)
        val angle: Float,
        @Id(5)
        val parent: LevelObject?) {

    private constructor() : this(ProjectileType.SMALL_BULLET, 0, 0, 0f, null)

    @Id(7)
    var xVel = 0f

    @Id(8)
    var yVel = 0f

    @Id(9)
    var xPixelLeftover = 0f

    @Id(10)
    var yPixelLeftover = 0f

    @Id(11)
    var ticksLived = 0

    init {
        val poly = Polygon()
        // create verticies
        val points = listOf(
                Point(-type.hitbox.width / 2, -type.hitbox.height / 2),
                Point(type.hitbox.width / 2, -type.hitbox.height / 2),
                Point(type.hitbox.width / 2, type.hitbox.height / 2),
                Point(-type.hitbox.width / 2, type.hitbox.height / 2)
        )
        // rotate verticies
        /*
        val rotatedPoints = points.map {
            Point(it.x * cos(angle) - it.y * sin(angle), it.x * sin(angle) + it.y * )
        }
         */
    }

    fun render() {
        Renderer.renderFilledRectangle(xPixel, yPixel, 8, 4, TextureRenderParams(rotation = Math.toDegrees(angle.toDouble()).toFloat()))
    }

    fun onCollide(o: LevelObject) {
        if (o != parent) {
            if (o is Entity) {
                o.health -= type.damage
                if (o.health <= 0) {
                    parent?.level?.remove(this)
                }
            }
            parent?.level?.remove(this)
        }
    }

    fun update() {
        ticksLived++
        if (ticksLived >= type.lifetime) {
            parent?.level?.remove(this)
            return
        }
        xVel = type.speed * cos(angle)
        yVel = type.speed * sin(angle)
        xPixelLeftover += xVel % 1
        yPixelLeftover += yVel % 1
        xPixel += xVel.toInt() + xPixelLeftover.toInt()
        yPixel += yVel.toInt() + yPixelLeftover.toInt()
        xPixelLeftover %= 1
        yPixelLeftover %= 1

        val collisions = parent?.level?.getCollisionsWithPoint(xPixel, yPixel)
        if (collisions != null && collisions.isNotEmpty()) {
            println("teskljajklfsdajkl")
        }
    }
}