package level.block

import com.badlogic.gdx.Input
import io.ControlEvent
import io.ControlEventType
import item.Inventory
import item.weapon.Weapon
import item.weapon.WeaponItemType
import level.entity.Entity
import level.getMovingObjectCollisionsInSquareCenteredOn
import resource.ResourceContainer
import resource.ResourceContainerChangeListener
import resource.ResourceList
import resource.ResourceNode

class ArmoryBlock(xTile: Int, yTile: Int) : MachineBlock(MachineBlockType.ARMORY, xTile, yTile),
    ResourceContainerChangeListener {

    val inventory = Inventory(1, 1)

    private constructor() : this(0, 0)

    override fun createNodes(): List<ResourceNode> {
        return listOf(ResourceNode(inventory, xTile, yTile))
    }

    override fun onInteractOn(
        event: ControlEvent,
        x: Int,
        y: Int,
        button: Int,
        shift: Boolean,
        ctrl: Boolean,
        alt: Boolean
    ) {
        if (event.type == ControlEventType.PRESS && !shift && !ctrl && !alt) {
            if (button == Input.Buttons.LEFT) {
                this.type.guiPool!!.toggle(this)
            }
        }
    }

    override fun onFinishWork() {
        val entitiesToBeArmored = level.getMovingObjectCollisionsInSquareCenteredOn(
            x + hitbox.width / 2,
            y + hitbox.height / 2,
            256
        ).filterIsInstance<Entity>()
        entitiesToBeArmored.forEach {
            if (it.weapon == null)
                it.weapon = Weapon(WeaponItemType.MACHINE_GUN)
        }
    }

    override fun onAddToContainer(container: ResourceContainer, resources: ResourceList) {
    }

    override fun onRemoveFromContainer(container: ResourceContainer, resources: ResourceList) {
    }

    override fun onContainerClear(container: ResourceContainer) {
    }
}