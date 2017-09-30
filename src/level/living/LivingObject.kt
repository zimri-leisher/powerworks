package level.living

import level.Hitbox
import level.moving.MovingObject

sealed class LivingType(val maxHealth: Int, val hitbox: Hitbox) {

}

sealed class RobotType(maxHealth: Int, hitbox: Hitbox, val armor: Int) : LivingType(maxHealth, hitbox) {
    constructor(parentType: LivingType, armor: Int) : this(parentType.maxHealth, parentType.hitbox, armor)
}

abstract class LivingObject(val type: LivingType, xPixel: Int, yPixel: Int) : MovingObject(xPixel, yPixel, type.hitbox) {

}