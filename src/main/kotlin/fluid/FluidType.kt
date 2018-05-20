package fluid

import graphics.Image
import item.IngotItemType
import resource.ResourceCategory
import resource.ResourceType

open class FluidType(initializer: FluidType.() -> Unit = {}) : ResourceType {

    override val category
        get() = ResourceCategory.FLUID
    override var icon = Image.Misc.ERROR
    override var name = "Error"

    init {
        ALL.add(this)
        initializer()
    }

    companion object {
        val ALL = mutableListOf<FluidType>()
    }
}

class MoltenOreFluidType(initializer: MoltenOreFluidType.() -> Unit) : FluidType() {

    var ingot = IngotItemType.IRON_INGOT

    init {
        initializer()
    }

    companion object {
        val MOLTEN_IRON = MoltenOreFluidType {
            name = "Molten Iron"
            icon = Image.Fluid.MOLTEN_IRON
            ingot = IngotItemType.IRON_INGOT
        }

        val MOLTEN_COPPER = MoltenOreFluidType {
            name = "Molten Copper"
            icon = Image.Fluid.MOLTEN_COPPER
            ingot = IngotItemType.COPPER_INGOT
        }
    }
}