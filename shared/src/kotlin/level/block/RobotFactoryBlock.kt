package level.block

import behavior.Behavior
import item.RobotItemType
import level.canAdd
import level.entity.EntityGroup
import misc.PixelCoord

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
                if (type is RobotItemType && level.canAdd(type.spawnedEntity, xPixel + 24, yPixel - 24)) {
                    val robot = type.spawnedEntity.instantiate(xPixel + 24, yPixel - 24, 0)
                    level.add(robot)
                    //newRobotGroup.add(robot)
                    //newRobotGroup.entities.forEach { it.behavior.run(Behavior.Movement.PATH_TO_FORMATION, argument = PixelCoord(robot.xPixel, robot.yPixel)) }
                } else {
                    outputContainer.add(type, quantity)
                }
            }
        } else {
            currentWork = type.maxWork
        }
    }
}