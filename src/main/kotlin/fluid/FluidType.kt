package fluid

import com.badlogic.gdx.graphics.Color
import graphics.Image
import item.IngotItemType
import main.toColor
import resource.ResourceCategory
import resource.ResourceType

open class FluidType(initializer: FluidType.() -> Unit = {}) : ResourceType() {

    override val category
        get() = ResourceCategory.FLUID
    override var icon = Image.Misc.ERROR
    override var name = "Error"

    var color = Color(0f, 0f, 0f, 1f)

    init {
        ALL.add(this)
        initializer()
    }

    companion object {
        val ALL = mutableListOf<FluidType>()
    }

    override fun toString() = name
}

class MoltenOreFluidType(initializer: MoltenOreFluidType.() -> Unit) : FluidType() {

    var ingot = IngotItemType.IRON_INGOT

    init {
        initializer()
    }

    companion object {
        val MOLTEN_IRON = MoltenOreFluidType {
            name = "Molten Iron"
            color = toColor(0xF8D700)
            icon = Image.Fluid.MOLTEN_IRON
            ingot = IngotItemType.IRON_INGOT
        }

        val MOLTEN_COPPER = MoltenOreFluidType {
            name = "Molten Copper"
            color = toColor(0XF8D000)
            icon = Image.Fluid.MOLTEN_COPPER
            ingot = IngotItemType.COPPER_INGOT
        }
    }
}