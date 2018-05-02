package level.living

import level.Hitbox
import level.moving.MovingObject

class LivingType(initializer: () -> Unit) {
    var maxHealth = 100
    var hitbox = Hitbox.TILE

}

abstract class LivingObject(val type: LivingType, xPixel: Int, yPixel: Int) : MovingObject(xPixel, yPixel, type.hitbox) {

}