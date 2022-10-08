package level.moving

import graphics.Image
import level.Hitbox
import level.PhysicalLevelObjectTextures
import level.PhysicalLevelObjectType
import screen.Camera

open class MovingObjectType<T : MovingObject>(initializer: MovingObjectType<T>.() -> Unit = {}) : PhysicalLevelObjectType<T>() {
    var maxSpeed = 10.0
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
            textures = PhysicalLevelObjectTextures(Image.Misc.ERROR)
            hitbox = Hitbox.NONE
        }

        val CAMERA = MovingObjectType<Camera> {
            maxSpeed = 1.5
            hitbox = Hitbox.NONE
        }
    }
}