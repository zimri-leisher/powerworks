package level.block

import behavior.Behavior
import item.RobotItemType
import level.canAdd
import level.entity.EntityGroup

class RobotFactoryBlock(xTile: Int, yTile: Int, rotation: Int) : CrafterBlock(CrafterBlockType.ROBOT_FACTORY, xTile, yTile, rotation) {

    val newRobotGroup = EntityGroup()
    var newRobotBehavior = Behavior.Movement.PATH_TO_FORMATION

    override fun onFinishWork() {
        if(recipe == null) {
            return
        }
        if (outputContainer.spaceFor(recipe!!.produce) && inputContainer.contains(recipe!!.consume)) {
            for ((type, quantity) in recipe!!.consume) {
                inputContainer.remove(type, quantity)
            }
            for ((type, quantity) in recipe!!.produce) {
                if (type is RobotItemType && level.canAdd(type.spawnedEntity, x + 24, y - 24)) {
                    val robot = type.spawnedEntity.instantiate(x + 24, y - 24, 0)
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