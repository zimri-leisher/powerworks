package level.block

import item.RobotItemType
import level.canAdd

class RobotFactoryBlock(xTile: Int, yTile: Int, rotation: Int) : CrafterBlock(CrafterBlockType.ROBOT_FACTORY, xTile, yTile, rotation) {
    override fun onFinishWork() {
        if (inputContainer.contains(recipe!!.consume)) {
            for (robotItemType in recipe!!.produce.keys.filterIsInstance<RobotItemType>()) {
                if (level.canAdd(robotItemType.spawnedEntity, xPixel + 24, yPixel - 24)) {
                    val spawnedRobot = robotItemType.spawnedEntity.instantiate(xPixel + 24, yPixel - 24, 0)
                    for ((type, quantity) in recipe!!.consume) {
                        inputContainer.remove(type, quantity)
                    }
                    level.add(spawnedRobot)
                }
            }
        } else {
            currentWork = type.maxWork
        }
    }
}