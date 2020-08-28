package level.block

import com.badlogic.gdx.Input
import io.ControlEvent
import io.ControlEventType
import item.weapon.Weapon
import item.weapon.WeaponItemType
import level.entity.Entity
import level.getMovingObjectCollisionsInSquareCenteredOn

class ArmoryBlock(xTile: Int, yTile: Int, rotation: Int) : MachineBlock(MachineBlockType.ARMORY, xTile, yTile, rotation) {

    private constructor() : this(0, 0, 0)

    override fun onInteractOn(event: ControlEvent, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
        if (event.type == ControlEventType.PRESS && !shift && !ctrl && !alt) {
            if (button == Input.Buttons.LEFT) {
                this.type.guiPool!!.toggle(this)
            }
        }
    }

    override fun onFinishWork() {
        val entitiesToBeArmored = level.getMovingObjectCollisionsInSquareCenteredOn(
                xPixel + hitbox.width / 2,
                yPixel + hitbox.height / 2,
                256).filterIsInstance<Entity>()
        entitiesToBeArmored.forEach {
            if (it.weapon == null)
                it.weapon = Weapon(WeaponItemType.MACHINE_GUN)
        }
    }
}