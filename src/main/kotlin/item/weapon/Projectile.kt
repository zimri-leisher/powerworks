package item.weapon

import graphics.Renderer
import graphics.TextureRenderParams
import level.Level
import level.LevelObject
import level.entity.Entity
import level.moving.MovingObject
import level.remove
import kotlin.math.cos
import kotlin.math.sin

class Projectile(val type: ProjectileType, var xPixel: Int, var yPixel: Int, val angle: Float, val parent: LevelObject) {

    var xVel = 0f
    var yVel = 0f
    var xPixelLeftover = 0f
    var yPixelLeftover = 0f

    var ticksLived = 0

    fun render() {
        Renderer.renderFilledRectangle(xPixel, yPixel, 8, 4, TextureRenderParams(rotation = Math.toDegrees(angle.toDouble()).toFloat()))
    }

    fun onCollide(o: LevelObject) {
        if(o != parent) {
            if(o is Entity) {
                o.health -= type.damage
                if(o.health <= 0) {
                    parent.level.remove(this)
                }
            }
            parent.level.remove(this)
        }
    }

    fun update() {
        ticksLived++
        if(ticksLived >= type.lifetime) {
            parent.level.remove(this)
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