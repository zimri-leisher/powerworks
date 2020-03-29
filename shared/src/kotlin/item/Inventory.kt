package item

import resource.*
import serialization.Id

class Inventory(
        @Id(11)
        val width: Int,
        @Id(7)
        val height: Int,
        @Id(8)
        private val items: Array<Item?> = arrayOfNulls(width * height)) : ResourceContainer(ResourceCategory.ITEM) {

    private constructor() : this(0, 0, arrayOf())

    val full: Boolean
        get() {
            if (items[items.lastIndex] != null) {
                return items.any { it!!.quantity != it.type.maxStack }
            }
            return false
        }

    @Id(9)
    val expected = ResourceList()

    @Id(10)
    override var totalQuantity = 0
        private set

    override fun add(resource: ResourceType, quantity: Int, from: ResourceNode?, checkIfAble: Boolean): Boolean {
        if (checkIfAble)
            if (!canAdd(resource, quantity))
                return false
        resource as ItemType
        if (expected.contains(resource)) {
            cancelExpectation(resource, quantity)
        }
        var amountLeftToAdd = quantity
        // fill out unmaxed stacks
        for (item in items) {
            if (item != null && item.type == resource && item.quantity < resource.maxStack) {
                if (amountLeftToAdd + item.quantity > resource.maxStack) {
                    amountLeftToAdd -= resource.maxStack - item.quantity
                    item.quantity = resource.maxStack
                } else {
                    item.quantity += amountLeftToAdd
                    totalQuantity += quantity
                    listeners.forEach { it.onContainerChange(this, resource, quantity) }
                    return true
                }
            }
        }
        // create new stacks
        for (i in items.indices) {
            if (items[i] == null) {
                val q = Math.min(resource.maxStack, amountLeftToAdd)
                items[i] = Item(resource, q)
                amountLeftToAdd -= q
            }
            if (amountLeftToAdd <= 0) {
                totalQuantity += quantity
                listeners.forEach { it.onContainerChange(this, resource, quantity) }
                return true
            }
        }
        return true
    }

    override fun expect(resource: ResourceType, quantity: Int): Boolean {
        if (!isRightType(resource))
            return false
        resource as ItemType
        if (spaceFor(expected + (resource to quantity))) {
            expected.add(resource, quantity)
            return true
        }
        return false
    }

    override fun cancelExpectation(resource: ResourceType, quantity: Int): Boolean {
        return expected.remove(resource, quantity)
    }

    override fun getQuantity(resource: ResourceType) = items.filter { it?.type == resource }.sumBy { it?.quantity ?: 0 }

    fun indexOf(resource: ItemType): Int {
        for (i in items.indices.reversed()) {
            if (items[i]?.type == resource) {
                return i
            }
        }
        return -1
    }

    fun add(i: Item): Boolean {
        return add(i.type, i.quantity, checkIfAble = true)
    }

    override fun spaceFor(list: ResourceList): Boolean {
        var extraStacks = 0
        for ((resource, quantity) in list) {
            if (!isRightType(resource)) {
                return false
            }
            resource as ItemType
            var capacity = 0
            for (i in items.indices) {
                val item = items[i]
                if (item != null) {
                    if (item.type == resource) {
                        capacity += resource.maxStack - item.quantity
                    }
                }
            }
            if (capacity < quantity) {
                val remaining = quantity - capacity
                extraStacks += Math.ceil(remaining.toDouble() / resource.maxStack).toInt()
            }
        }
        return items.sumBy { if (it == null) 1 else 0 } >= extraStacks
    }

    override fun remove(resource: ResourceType, quantity: Int, to: ResourceNode?, checkIfAble: Boolean): Boolean {
        if (checkIfAble)
            if (!canRemove(resource, quantity))
                return false
        var amountLeftToRemove = quantity
        for (i in items.indices.reversed()) {
            val item = items[i]
            if (item != null && item.type == resource) {
                val prevQ = item.quantity
                item.quantity = Math.max(item.quantity - amountLeftToRemove, 0)
                if (item.quantity <= 0) {
                    items[i] = null
                }
                amountLeftToRemove -= prevQ - item.quantity
                if (amountLeftToRemove <= 0) {
                    totalQuantity -= quantity
                    listeners.forEach { it.onContainerChange(this, resource, -quantity) }
                    return true
                }
            }
        }
        // not going to throw an exception here. Once past the checkIfAble, we assume that we are going to succeed blindly
        return true
    }

    fun remove(i: Item): Boolean {
        return remove(i.type, i.quantity)
    }

    override fun contains(list: ResourceList): Boolean {
        for ((resource, quantity) in list) {
            var contains = 0
            for (i in items.indices) {
                if (items[i] != null) {
                    val item = items[i]!!
                    if (item.type == resource) {
                        contains += item.quantity
                    }
                }
            }
            if (contains < quantity)
                return false
        }
        return true
    }

    override fun resourceList(): ResourceList {
        val map = mutableMapOf<ResourceType, Int>()
        for (item in items) {
            if (item != null) {
                if (item.type in map) {
                    val newQ = map.get(item.type)!! + item.quantity
                    map.replace(item.type, newQ)
                } else {
                    map.put(item.type, item.quantity)
                }
            }
        }
        return ResourceList(map)
    }

    override fun typeList() = items.mapNotNull { it?.type }.toSet()

    /**
     * Inclusive, goes to the end of items from this index
     */
    private fun shiftRight(index: Int, num: Int): Int {
        var count = 0
        // Don't worry about out of bounds, we assume that we've already checked for space
        while (count < num) {
            for (i in items.lastIndex downTo (index + 1)) {
                items[i] = items[i - 1]
            }
            count++
        }
        return count
    }

    /**
     * Inclusive, goes to the end of items from this index
     */
    private fun shiftLeft(index: Int, num: Int): Int {
        var count = 0
        // Don't worry about out of bounds, we assume that we've already checked for space
        while (count < num) {
            for (i in index until (items.lastIndex - 1)) {
                items[i] = items[i + 1]
            }
            count++
        }
        items[items.lastIndex - 1] = null
        return count
    }

    override fun clear() {
        for (i in items.indices) {
            items[i] = null
        }
        listeners.forEach { it.onContainerClear(this) }
    }

    override fun copy(): Inventory {
        val inv = Inventory(width, height, items.copyOf())
        return inv
    }

    operator fun iterator() = items.iterator()

    operator fun get(i: Int) = items[i]

    operator fun set(i: Int, v: Item?) {
        if (items[i] != null)
            totalQuantity -= items[i]!!.quantity
        items[i] = v
        totalQuantity += v?.quantity ?: 0
    }

    override fun toString() = "Inventory width: $width, height: $height, $totalQuantity items"

    fun contentEquals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as Inventory

        if (width != other.width) return false
        if (height != other.height) return false
        if (!items.contentEquals(other.items)) return false
        if (expected != other.expected) return false
        if (totalQuantity != other.totalQuantity) return false

        return true
    }

    fun contentHashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + items.contentHashCode()
        result = 31 * result + expected.hashCode()
        result = 31 * result + totalQuantity
        return result
    }
}
