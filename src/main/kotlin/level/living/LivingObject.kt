package level.living

import graphics.Image
import level.LevelObjectTextures
import level.LevelObjectType
import level.moving.MovingObject

open class LivingType<T : LivingObject>(initializer: () -> Unit = {}) : LevelObjectType<T>() {
    var maxHealth = 100

    init {
        initializer()
    }

    companion object {
        val ERROR = LivingType<LivingObject>()
    }
}

abstract class LivingObject(type: LivingType<out LivingObject>, xPixel: Int, yPixel: Int, rotation: Int = 0) : MovingObject(type, xPixel, yPixel, rotation, type.hitbox) {
    override val type = type
}