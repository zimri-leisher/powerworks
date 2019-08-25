package item.weapon

import graphics.Renderer
import graphics.TextureRenderParams
import level.Level
import level.LevelObject
import level.entity.Entity
import level.moving.MovingObject
import kotlin.math.cos
import kotlin.math.sin

class Projectile(type: ProjectileType, xPixel: Int, yPixel: Int, rotation: Int, val angle: Float, val parent: LevelObject) : MovingObject(type, xPixel, yPixel, rotation) {
    override var type = type

    var ticksLived = 0

    override fun render() {
        Renderer.renderFilledRectangle(xPixel, yPixel, 8, 4, TextureRenderParams(rotation = Math.toDegrees(angle.toDouble()).toFloat()))
    }

    override fun onCollide(o: LevelObject) {
        if(o != parent) {
            if(o is Entity) {
                o.health -= type.damage
                if(o.health <= 0) {
                    Level.remove(o)
                }
            }
            Level.remove(this)
        }
    }

    override fun update() {
        ticksLived++
        if(ticksLived >= type.lifetime) {
            Level.remove(this)
        } else {
            xVel = (type.speed * cos(angle)).toInt()
            yVel = (type.speed * sin(angle)).toInt()
            super.update()
        }
    }
}