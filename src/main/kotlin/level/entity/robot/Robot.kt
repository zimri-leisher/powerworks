package level.entity.robot

import behavior.leaves.FindPath
import io.*
import level.LevelObject
import level.entity.Entity
import main.Game
import misc.Numbers
import kotlin.math.PI
import kotlin.math.atan

open class Robot(type: RobotType<out Robot>, xPixel: Int, yPixel: Int, rotation: Int = 0) : Entity(type, xPixel, yPixel, rotation), ControlPressHandler {

    override fun handleControlPress(p: ControlPress) {
        if(p.pressType == PressType.PRESSED && inLevel) {
            val xPixel = Game.currentLevel.mouseLevelXPixel
            val yPixel = Game.currentLevel.mouseLevelYPixel
            val xDiff = xPixel - this.xPixel
            val yDiff = yPixel - this.yPixel
            val angle = atan(yDiff.toDouble() / xDiff) + if(Numbers.sign(xDiff) == -1) PI else 0.0
            //attack(angle.toFloat())
        }
    }

    override val type = type

    init {
        InputManager.registerControlPressHandler(this, ControlPressHandlerType.LEVEL_ANY, Control.SECONDARY_INTERACT)
    }

     fun attack(target: LevelObject) {
        if(weapon != null) {
            //Level.add(Projectile(weapon!!.projectileType, xPixel, yPixel, 0, angle, this))
        }
    }
}