package level.entity.robot

import io.*
import item.weapon.Weapon
import item.weapon.WeaponItemType
import level.entity.Entity
import main.Game

open class Robot(type: RobotType<out Robot>, xPixel: Int, yPixel: Int, rotation: Int = 0) : Entity(type, xPixel, yPixel, rotation), ControlHandler {

    override val type = type

    init {
        if(!Game.IS_SERVER) {
            InputManager.registerControlPressHandler(this, ControlPressHandlerType.LEVEL_ANY_UNDER_MOUSE, Control.SECONDARY_INTERACT)
        }
    }

    override fun onAddToLevel() {
        super.onAddToLevel()
        weapon = Weapon(WeaponItemType.MACHINE_GUN)
    }

    override fun handleControl(p: ControlPress) {
        if(p.pressType == PressType.PRESSED && inLevel) {
            
        }
    }

    override fun toString(): String {
        return "Robot at $xPixel, $yPixel"
    }
}