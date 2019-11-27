package resource

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import graphics.Renderable

private var nextId = 0

/**
 * A type of resource, e.g. [item.ItemType.CABLE] or [fluid.MoltenOreFluidType.MOLTEN_COPPER]
 */
abstract class ResourceType {
    abstract val icon: Renderable
    abstract val category: ResourceCategory
    abstract val name: String

    val id = nextId++

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

        fun possibleResourceTypes(name: String) = ALL.filter { it.name.contains(name) }
        /**
         * This ignores case
         */
        fun getType(name: String) = ALL.firstOrNull { it.name.toLowerCase() == name.toLowerCase() }
    }
}

class ResourceTypeSerializer : Serializer<ResourceType>() {
    override fun write(kryo: Kryo, output: Output, `object`: ResourceType) {
        output.writeInt(`object`.id)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out ResourceType>): ResourceType {
        val id = input.readInt()
        return ResourceType.ALL.first { it.id == id }.apply { kryo.reference(this) }
    }

}