package level.entity.robot

import io.*
import level.entity.Entity
import main.Game

open class Robot(type: RobotType<out Robot>, xPixel: Int, yPixel: Int, rotation: Int = 0) : Entity(type, xPixel, yPixel, rotation) {

    override val type = type

    override fun toString(): String {
        return "Robot at $xPixel, $yPixel"
    }
}