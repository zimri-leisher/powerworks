package resource

import serialization.Id
import java.lang.Integer.min

fun emptyResourceList() = EmptyResourceList

fun resourceListOf(vararg pairs: Pair<ResourceType, Int>) = ResourceList(*pairs)

fun Map<ResourceType, Int>.toResourceList() = ResourceList(this)

/**
 * A list of [ResourceType] - quantity pairs with some convenience methods
 */
open class ResourceList(
    @Id(1) protected val resources: Map<ResourceType, Int> = mapOf()
) : Map<ResourceType, Int> {

    constructor(vararg pairs: Pair<ResourceType, Int>) : this(pairs.toMap())

    override val size: Int
        get() = resources.size

    @Id(2)
    open val totalQuantity = resources.values.sum()

    override val entries: Set<Map.Entry<ResourceType, Int>>
        get() = resources.entries
    override val keys: Set<ResourceType>
        get() = resources.keys
    override val values: Collection<Int>
        get() = resources.values

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
    fun containsAtLeastAll(other: Map<out ResourceType, Int>) =
        other.all { (type, quantity) -> containsAtLeast(type, quantity) }

    /**
     * @return true if every (type, quantity) entry in [other] passes [containsAtMost]
     */
    fun containsAtMostAll(other: Map<out ResourceType, Int>) =
        other.all { (type, quantity) -> containsAtMost(type, quantity) }

    fun intersection(other: Map<out ResourceType, Int>) =
        ResourceList(other.entries.associate { (k, v) -> k to (min(v, get(k))) })

    override fun get(key: ResourceType) = resources[key] ?: 0

    operator fun get(index: Int) = resources.entries.toList()[index]

    override fun isEmpty() = resources.isEmpty()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ResourceList) return false

        if (other.resources.size != resources.size) return false
        for ((type, quantity) in other.resources) {
            if (!containsExactly(type, quantity)) {
                return false
            }
        }

        return true
    }

    open operator fun plus(other: ResourceList): ResourceList {
        return ResourceList(other.resources + resources)
    }

    override fun hashCode(): Int {
        return resources.hashCode()
    }

    override fun toString(): String {
        return resources.entries.joinToString()
    }

    open fun copy() = ResourceList(*resources.entries.map { (key, value) -> key to value }.toTypedArray())
}

object EmptyResourceList : ResourceList()