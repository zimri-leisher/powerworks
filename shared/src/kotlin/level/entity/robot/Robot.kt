package level.entity.robot

import level.entity.Entity

open class Robot(type: RobotType<out Robot>, x: Int, y: Int) : Entity(type, x, y) {

    private constructor() : this(RobotType.STANDARD, 0, 0)

    override val type = type

    override fun toString(): String {
        return "Robot at $x, $y"
    }
}