package resource

import java.lang.Integer.min

fun mutableResourceListOf(vararg pairs: Pair<ResourceType, Int>) = MutableResourceList(*pairs)

fun Map<ResourceType, Int>.toMutableResourceList() =
    if (this is MutableResourceList) this.copy() else MutableResourceList(*this.entries.map { (key, value) -> key to value }
        .toTypedArray())

fun Collection<ResourceStack>.toMutableResourceList() = MutableResourceList(this)


/**
 * A list of [ResourceType] - quantity pairs with some convenience methods. Use [take] and [put] to do operations
 * logically for resources. [add], [remove] only add/remove exact matches.
 */
class MutableResourceList(
    resources: MutableMap<ResourceType, Int> = mutableMapOf()
) : ResourceList(resources), MutableMap<ResourceType, Int>, MutableSet<Map.Entry<ResourceType, Int>> {

    private val _mutableResources get() = resources as MutableMap<ResourceType, Int>

    override var totalQuantity = super.totalQuantity
        private set

    constructor(resources: ResourceList) : this(resources.toMutableMap())
    constructor(vararg pairs: Pair<ResourceType, Int>) : this(pairs.toMap().toMutableMap())
    constructor(vararg entries: ResourceStack) : this() {
        for (entry in entries) {
            put(entry.type, entry.quantity)
        }
    }

    constructor(entries: Collection<ResourceStack>) : this() {
        for (entry in entries) {
            put(entry.type, entry.quantity)
        }
    }

    override val entries: MutableSet<MutableMap.MutableEntry<ResourceType, Int>>
        get() = _mutableResources.entries
    override val keys: MutableSet<ResourceType>
        get() = _mutableResources.keys
    override val values: MutableCollection<Int>
        get() = _mutableResources.values

    override fun containsKey(key: ResourceType) = resources.containsKey(key)

    override fun containsValue(value: Int) = resources.containsValue(value)
    override fun add(element: Map.Entry<ResourceType, Int>) = put(element.type, element.quantity) != 0

    override fun addAll(elements: Collection<Map.Entry<ResourceType, Int>>) = elements.all { add(it) }

    override fun clear() {
        _mutableResources.clear()
        totalQuantity = 0
    }

    override fun iterator(): MutableIterator<Map.Entry<ResourceType, Int>> = entries.iterator()

    override fun retainAll(elements: Collection<Map.Entry<ResourceType, Int>>) = entries.retainAll(elements)

    override fun removeAll(elements: Collection<Map.Entry<ResourceType, Int>>) = entries.removeAll(elements)

    override fun remove(element: Map.Entry<ResourceType, Int>) = entries.remove(element)

    /**
     * Adds [value] of [key] to this list. If there is already an entry for [key], it will just add on [value] to its value.
     * Otherwise, makes a new entry for the [key].
     * @return the previously existing quantity under [key], or 0 if there was none
     */
    override fun put(key: ResourceType, value: Int): Int {
        val positiveValue = value.coerceAtLeast(0)
        val alreadyExistingQuantity = get(key)
        if (alreadyExistingQuantity > 0) {
            _mutableResources.replace(key, alreadyExistingQuantity + positiveValue)
            totalQuantity += positiveValue
            return alreadyExistingQuantity
        } else {
            _mutableResources.put(key, positiveValue)
            totalQuantity += positiveValue
            return 0
        }
    }

    /**
     * Adds all resources from the [from] list to this list. See [add] for specifics on what happens
     */
    override fun putAll(from: Map<out ResourceType, Int>) {
        for ((key, value) in from) {
            put(key, value)
        }
    }

    /**
     * Removes all of the given [ResourceType] from this list
     */
    override fun remove(key: ResourceType): Int? {
        val existing = _mutableResources.remove(key)
        totalQuantity -= existing ?: 0
        return existing
    }

    /**
     * Removes [quantity] of the [type] from this resource list. If this resource list contains less than [quantity] of
     * [type], it removes as much as it can. If the resulting quantity is 0, it removes the [type] entry from this list.
     * @return true if any resources were removed
     */
    fun take(type: ResourceType, quantity: Int): Boolean {
        val currentQuantity = get(type)
        if (currentQuantity == 0 || quantity == 0) {
            return false
        }
        totalQuantity -= min(currentQuantity, quantity)
        if (currentQuantity <= quantity) {
            remove(type)
        } else {
            _mutableResources.replace(type, currentQuantity - quantity)
        }
        return true
    }

    /**
     * Removes the [resources] from this resource list.
     * @return true if any resources were removed
     */
    fun takeAll(resources: ResourceList) =
        (resources as Map<ResourceType, Int>).any { (type, quantity) -> take(type, quantity) }

    override operator fun plus(other: ResourceList): MutableResourceList {
        val list = mutableResourceListOf()
        list.putAll(other)
        list.putAll(this)
        return list
    }

    override fun copy() = MutableResourceList(*resources.entries.map { (key, value) -> key to value }.toTypedArray())
}