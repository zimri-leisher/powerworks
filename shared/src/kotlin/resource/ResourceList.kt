package resource

import serialization.Id


/**
 * A list of [ResourceType] - quantity pairs with some convenience methods
 */
class ResourceList(
        @Id(1)
        private val resources: MutableMap<ResourceType, Int> = mutableMapOf()
) : Map<ResourceType, Int> {

    constructor(vararg pairs: Pair<ResourceType, Int>) : this(pairs.toMap().toMutableMap())

    override val size: Int
        get() = resources.size

    val totalQuantity get() = values.sum()

    override fun containsKey(key: ResourceType) = resources.containsKey(key)

    override fun containsValue(value: Int) = resources.containsValue(value)

    /**
     * @return true if this list contains [quantity] or greater of the specified [type], or the [quantity] is 0. False if this list doesn't have
     * [quantity] of [type], or if the list has no entry for [type]
     */
    fun containsAtLeast(type: ResourceType, quantity: Int): Boolean {
        if (quantity == 0) return true
        val value = resources[type] ?: return false
        return value >= quantity
    }

    /**
     * @return true if this list contains [quantity] or less of the specified [type], or if the list has no entry for that [type]
     */
    fun containsAtMost(type: ResourceType, quantity: Int): Boolean {
        if (quantity == 0) return true
        val value = resources[type] ?: return true
        return value <= quantity
    }

    /**
     * @return true if this list contains exactly [quantity] of [type]
     */
    fun containsExactly(type: ResourceType, quantity: Int) = get(type) == quantity

    /**
     * @return true if every (type, quantity) entry in [other] passes [containsAtLeast]
     */
    fun containsAtLeastAll(other: Map<out ResourceType, Int>) = other.all { (type, quantity) -> containsAtLeast(type, quantity) }

    /**
     * @return true if every (type, quantity) entry in [other] passes [containsAtMost]
     */
    fun containsAtMostAll(other: Map<out ResourceType, Int>) = other.all { (type, quantity) -> containsAtMost(type, quantity) }

    override fun get(key: ResourceType) = resources[key] ?: 0

    operator fun get(index: Int) = if (index > size - 1) null else resources.entries.toMutableList()[index]

    override fun isEmpty() = resources.isEmpty()

    override val entries: MutableSet<MutableMap.MutableEntry<ResourceType, Int>>
        get() = resources.entries
    override val keys: MutableSet<ResourceType>
        get() = resources.keys
    override val values: MutableCollection<Int>
        get() = resources.values

    fun clear() = resources.clear()

    /**
     * Adds [quantity] of [type] to this list. If there is already an entry for [type], it will just add on [quantity] to its value.
     * Otherwise, makes a new entry for the [type].
     * @return the previously existing quantity under [type], or null if there was none
     */
    fun add(type: ResourceType, quantity: Int): Int? {
        val alreadyExistingQuantity = get(type)
        if (alreadyExistingQuantity != 0) {
            resources.replace(type, alreadyExistingQuantity + quantity)
            return alreadyExistingQuantity
        } else {
            resources.put(type, quantity)
            return null
        }
    }

    /**
     * Adds all resources from the [from] list to this list. See [add] for specifics on what happens
     */
    fun addAll(from: Map<out ResourceType, Int>) {
        for ((key, value) in from) {
            add(key, value)
        }
    }

    /**
     * Removes the (key, value) entry with the given [key] from this list
     */
    fun remove(key: ResourceType) = resources.remove(key)

    /**
     * Removes [quantity] of the [type] from this resource list. If this resource list contains less than [quantity] of
     * [type], it removes as much as it can. If the resulting quantity is 0, it removes the [type] entry from this list.
     * @return true if any resources were removed
     */
    fun take(type: ResourceType, quantity: Int): Boolean {
        val currentQuantity = get(type)
        if(currentQuantity == 0 || quantity == 0) {
            return false
        }
        if (currentQuantity <= quantity) {
            remove(type)
        } else {
            resources.replace(type, currentQuantity - quantity)
        }
        return true
    }

    /**
     * Removes the [resources] from this resource list.
     * @return true if any resources were removed
     */
    fun takeAll(resources: ResourceList) = resources.all { (type, quantity) -> take(type, quantity) }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ResourceList

        if (other.resources.size != resources.size) return false
        for ((type, quantity) in other.resources) {
            if (!containsExactly(type, quantity)) {
                return false
            }
        }

        return true
    }

    operator fun plus(other: ResourceList): ResourceList {
        val list = ResourceList()
        list.addAll(other)
        list.addAll(this)
        return list
    }

    override fun hashCode(): Int {
        return resources.hashCode()
    }

    override fun toString(): String {
        return resources.entries.joinToString()
    }

}