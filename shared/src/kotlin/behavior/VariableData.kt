package behavior

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer
import level.entity.Entity
import main.removeIfKey
import serialization.Id

class VariableData {
    @Id(1)
    private val data = mutableMapOf<String, Any?>()

    fun set(node: Node? = null, entity: Entity? = null, name: String, value: Any?): Any? {
        val key = getKey(node, entity, name)
        return data.put(key, value)
    }

    fun <T> get(node: Node? = null, entity: Entity? = null, name: String): T? {
        val key = getKey(node, entity, name)
        return data[key] as T?
    }

    fun exists(node: Node? = null, entity: Entity? = null, name: String) = data.containsKey(getKey(node, entity, name))

    /**
     * Deletes the entry in the data map corresponding with this [node], [entity] and [name]
     */
    fun deleteCorresponding(node: Node? = null, entity: Entity? = null, name: String) = data.removeIfKey { it == getKey(node, entity, name) }

    /**
     * Deletes all entries in the data map that were stored under the [entity]
     */
    fun deleteCorresponding(entity: Entity) = data.removeIfKey { it.split("-").map { it.trim('-') }.contains(getEntityString(entity)) }

    /**
     * Deletes all entries in the data map that were stored under the [node]
     */
    fun deleteCorresponding(node: Node) = data.removeIfKey { it.split("-").map { it.trim('-') }.contains(getNodeString(node)) }

    private fun getKey(node: Node?, entity: Entity?, name: String): String {
        return "${getNodeString(node)}-${getEntityString(entity)}-$name"
    }

    fun getNodeString(node: Node?) = if(node != null) node::class.java.simpleName + ":" + node.id else ""
    fun getEntityString(entity: Entity?) = if(entity != null) entity::class.java.simpleName + ":" + entity.id else ""
}