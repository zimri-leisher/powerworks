package level.block

import behavior.Behavior
import item.Inventory
import item.RobotItemType
import level.canAdd
import level.entity.EntityGroup
import resource.ResourceNode

class RobotFactoryBlock(xTile: Int, yTile: Int) : CrafterBlock(CrafterBlockType.ROBOT_FACTORY, xTile, yTile) {

    private constructor() : this(0, 0)

    val newRobotGroup = EntityGroup()
    var newRobotBehavior = Behavior.Movement.PATH_TO_FORMATION

    val inventory = Inventory(type.internalStorageSize, 1)
    val output = Inventory(1, 1)

    override fun createNodes(): List<ResourceNode> {
        return listOf(
            ResourceNode(inventory, xTile, yTile + 1),
            ResourceNode(inventory, xTile + 2, yTile + 2),
            ResourceNode(output, xTile + 1, yTile)
        )
    }

    override fun onFinishWork() {
        if (recipe == null) {
            return
        }
        if (outputContainer.canAdd(recipe!!.produce) && inputContainer.canRemove(recipe!!.consume)) {
            for ((type, quantity) in recipe!!.consume) {
                inputContainer.remove(type, quantity)
            }
            for ((type, quantity) in recipe!!.produce) {
                if (type is RobotItemType && level.canAdd(type.spawnedEntity, x + 24, y - 24)) {
                    val robot = type.spawn(x + 24, y - 24)
                    level.add(robot)
                } else {
                    outputContainer.add(type, quantity)
                }
            }
        } else {
            currentWork = type.maxWork
        }
    }
}