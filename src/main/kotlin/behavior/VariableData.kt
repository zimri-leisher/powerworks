package behavior

import level.entity.Entity

class VariableData {
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

    fun deleteCorresponding(entity: Entity) {
        val iterator = data.iterator()
        for((k, v) in iterator) {
            if(k.split('-').contains(getEntityString(entity))) {
                iterator.remove()
            }
        }
    }


    private fun getKey(node: Node?, entity: Entity?, name: String?): String {
        return "${getNodeString(node)}-${getEntityString(entity)}-$name"
    }

    fun getNodeString(node: Node?) = if(node != null) node::class.java.simpleName + ":" + node.id else ""
    fun getEntityString(entity: Entity?) = if(entity != null) entity::class.java.simpleName + ":" + entity.id else ""
}