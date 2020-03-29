package item.weapon

import graphics.Renderer
import graphics.TextureRenderParams
import level.LevelObject
import level.entity.Entity
import serialization.Id
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
        } else {
            xVel = type.speed * cos(angle)
            yVel = type.speed * sin(angle)
            xPixelLeftover += xVel % 1
            yPixelLeftover += yVel % 1
            xPixel += xVel.toInt() + xPixelLeftover.toInt()
            yPixel += yVel.toInt() + yPixelLeftover.toInt()
            xPixelLeftover %= 1
            yPixelLeftover %= 1
        }
    }
}