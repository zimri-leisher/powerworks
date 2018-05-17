package level.living

import level.Hitbox
import level.LevelObjectTextures
import level.LevelObjectType
import level.moving.MovingObject

open class LivingType<T : LivingObject>(initializer: () -> Unit) : LevelObjectType<T>() {
    var maxHealth = 100
    var textures = LevelObjectTextures()

    init {
        initializer()
    }
}

abstract class LivingObject(type: LivingType<out LivingObject>, xPixel: Int, yPixel: Int, rotation: Int = 0) : MovingObject(type, xPixel, yPixel, rotation, type.hitbox) {
    override val type = type
}