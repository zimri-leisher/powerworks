package level.block

import crafting.Crafter

class RobotFactoryBlock(xTile: Int, yTile: Int, rotation: Int) : MachineBlock(MachineBlockType.ROBOT_FACTORY, xTile, yTile, rotation), Crafter {
    override val crafterType: Int
        get() = Crafter.ROBOT_CRAFTER
}