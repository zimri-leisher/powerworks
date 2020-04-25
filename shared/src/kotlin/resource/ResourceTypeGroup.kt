package resource

import fluid.MoltenOreFluidType
import item.BlockItemType
import item.OreItemType
import item.RobotItemType

enum class ResourceTypeGroup(val types: List<ResourceType>, val displayName: String) {
    MOLTEN_ORE_FLUIDS(MoltenOreFluidType.ALL, "Molten Ore Fluids"),
    ORE_ITEMS(OreItemType.ALL, "Ores"),
    ROBOT_ITEMS(RobotItemType.ALL, "Robots"),
    MACHINE_BLOCK_ITEMS(BlockItemType.ALL.filter { it.placedBlock is level.block.MachineBlockType<*> }, "Machines")
    ;
    constructor(vararg types: ResourceType, displayName: String) : this(types.toList(), displayName)

    override fun toString(): String {
        return displayName
    }
}