package level.entity.robot

import io.*
import level.LevelObject
import level.entity.Entity
import misc.Numbers
import kotlin.math.PI
import kotlin.math.atan
import level.LevelManager
import main.Game

open class Robot(type: RobotType<out Robot>, xPixel: Int, yPixel: Int, rotation: Int = 0) : Entity(type, xPixel, yPixel, rotation), ControlPressHandler {

    override val type = type

    init {
        if(!Game.IS_SERVER) {
            InputManager.registerControlPressHandler(this, ControlPressHandlerType.LEVEL_ANY_UNDER_MOUSE, Control.SECONDARY_INTERACT)
        }
    }

    override fun handleControlPress(p: ControlPress) {
        if(p.pressType == PressType.PRESSED && inLevel) {
            val xPixel = LevelManager.mouseLevelXPixel
            val yPixel = LevelManager.mouseLevelYPixel
            val xDiff = xPixel - this.xPixel
            val yDiff = yPixel - this.yPixel
            val angle = atan(yDiff.toDouble() / xDiff) + if(Numbers.sign(xDiff) == -1) PI else 0.0
            //attack(angle.toFloat())
        }
    }

     fun attack(target: LevelObject) {
        if(weapon != null) {
            //Level.add(Projectile(weapon!!.projectileType, xPixel, yPixel, 0, angle, this))
        }
    }
}