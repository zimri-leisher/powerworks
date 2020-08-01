package level.block

import item.RobotItemType
import level.canAdd

class RobotFactoryBlock(xTile: Int, yTile: Int, rotation: Int) : CrafterBlock(CrafterBlockType.ROBOT_FACTORY, xTile, yTile, rotation) {

    override fun onFinishWork() {
        if(recipe == null) {
            return
        }
        if (true || outputContainer.spaceFor(recipe!!.produce) && inputContainer.contains(recipe!!.consume)) {
            for ((type, quantity) in recipe!!.consume) {
                inputContainer.remove(type, quantity)
            }
            for ((type, quantity) in recipe!!.produce) {
                if (type is RobotItemType && level.canAdd(type.spawnedEntity, xPixel + 24, yPixel - 24)) {
                    level.add(type.spawnedEntity.instantiate(xPixel + 24, yPixel - 24, 0))
                } else {
                    outputContainer.add(type, quantity)
                }
            }
        } else {
            currentWork = type.maxWork
        }
    }
}