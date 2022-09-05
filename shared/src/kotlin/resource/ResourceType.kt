package resource

import graphics.Renderable
import java.util.*

private var nextId = 0

/**
 * A type of resource, e.g. [item.ItemType.CABLE] or [fluid.MoltenOreFluidType.MOLTEN_COPPER]
 */
abstract class ResourceType {
    abstract val icon: Renderable
    abstract val category: ResourceCategory
    abstract val name: String

    val technicalName get() = name.lowercase(Locale.getDefault()).replace(" ", "_")

    /**
     * Whether to show this when searching all resource types
     */
    var hidden = false

    val id = ResourceNetwork_nextId++

    init {
        ALL.add(this)
    }

    override fun equals(other: Any?): Boolean {
        return other is ResourceType && other.id == this.id
    }

    override fun hashCode(): Int {
        return id
    }

    companion object {
        val ALL = mutableListOf<ResourceType>()

        fun getPossibleTypes(name: String) = ALL.filter {
            it.name.lowercase(Locale.getDefault()).contains(name) or it.technicalName.contains(name) }

        fun getType(name: String) = ALL.firstOrNull { it.name.lowercase(Locale.getDefault()) == name || it.technicalName == name }
    }
}