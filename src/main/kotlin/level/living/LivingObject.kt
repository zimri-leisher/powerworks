package level.living

import level.LevelObjectType
import level.living.behavior.LivingObjectBehavior
import level.moving.MovingObject

open class LivingType<T : LivingObject>(initializer: () -> Unit = {}) : LevelObjectType<T>() {
    var maxHealth = 100

    init {
        requiresUpdate = true
        requiresRender = true
        initializer()
    }

    companion object {
        val ERROR = LivingType<LivingObject>()
    }
}

abstract class LivingObject(type: LivingType<out LivingObject>, xPixel: Int, yPixel: Int, rotation: Int = 0) : MovingObject(type, xPixel, yPixel, rotation, type.hitbox) {
    override val type = type
    var health = type.maxHealth
    var behavior = LivingObjectBehavior(this)

    override fun update() {
        behavior.update()
        super.update()
    }
}