package resource

class ResourceList(private val resources: MutableMap<ResourceType, Int> = mutableMapOf()) {

    constructor(vararg pairs: Pair<ResourceType, Int>) : this(pairs.toMap().toMutableMap())

    /**
     * If there are more resources of each in this list in the other list
     */
    fun enoughIn(other: ResourceList): Boolean {
        for ((k, v) in resources) {
            if (!other.resources.containsKey(k) || other.resources.get(k)!! < v)
                return false
        }
        return true
    }

    fun forEach(f: (ResourceType, Int) -> Unit) = resources.forEach(f)

    fun any(f: (Map.Entry<ResourceType, Int>) -> Boolean) = resources.any(f)

    fun addAll(other: ResourceList) {
        for((r, q) in other.resources) {
            add(r, q)
        }
    }

    fun add(resource: ResourceType, quantity: Int) {
        if(resource in resources) {
            resources.replace(resource, resources.get(resource)!! + quantity)
        } else {
            resources.put(resource, quantity)
        }
    }

    /**
     * If this doesn't contain enough, IT WILL STILL REMOVE ALL IT CAN!
     */
    fun remove(resource: ResourceType, quantity: Int): Boolean {
        if(resource in resources) {
            if(resources[resource]!! <= quantity) {
                resources.remove(resource)
                return false
            } else {
                resources.replace(resource, resources[resource]!! - quantity)
                return true
            }
        }
        return false
    }

    operator fun get(i: Int): Pair<ResourceType, Int>? = if(i >= resources.size) null else resources.entries.toMutableList()[i].toPair()

    fun getQuantity(resource: ResourceType) = resources[resource] ?: 0

    operator fun iterator() = resources.iterator()

    fun clear() = resources.clear()

    operator fun contains(resource: ResourceType) = resources.containsKey(resource)

    override fun toString() = resources.toString()

    val size get() = resources.size

    override fun equals(other: Any?): Boolean {
        if(other === this)
            return true
        return other is ResourceList && other.resources == resources
    }

    override fun hashCode(): Int {
        return resources.hashCode()
    }
}