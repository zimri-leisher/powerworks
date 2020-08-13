package level.moving

import graphics.Image
import level.Hitbox
import level.LevelObjectTextures
import level.LevelObjectType
import screen.Camera

open class MovingObjectType<T : MovingObject>(initializer: MovingObjectType<T>.() -> Unit = {}) : LevelObjectType<T>() {
    var maxSpeed = 10
    var drag = 2
    var density = 1.0
    var mass = -1.0

    init {
        requiresUpdate = true
        initializer()
        mass = hitbox.width * hitbox.height * density
        ALL.add(this)
    }

    companion object {

        val ALL = mutableListOf<MovingObjectType<*>>()

        val ERROR = MovingObjectType<DefaultMovingObject> {
            instantiate = { xPixel, yPixel, rotation -> DefaultMovingObject(this, xPixel, yPixel, rotation) }
            textures = LevelObjectTextures(Image.Misc.ERROR)
            hitbox = Hitbox.NONE
        }

        val CAMERA = MovingObjectType<Camera> {
            hitbox = Hitbox.NONE
            instantiate = { xPixel, yPixel, _ -> Camera(xPixel, yPixel) }
        }
    }
}