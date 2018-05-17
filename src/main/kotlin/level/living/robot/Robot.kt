package level.living.robot

import level.living.LivingObject

open class Robot(type: RobotType<out Robot>, xPixel: Int, yPixel: Int, rotation: Int = 0) : LivingObject(type, xPixel, yPixel, rotation) {
    override val type = type
}