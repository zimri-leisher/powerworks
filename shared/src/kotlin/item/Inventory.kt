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
    override val expected = ResourceList()

    @Id(10)
    override var totalQuantity = 0
        private set

    override fun add(resources: ResourceList, from: ResourceNode?, checkIfAble: Boolean): Boolean {
        if (checkIfAble)
            if (!canAdd(resources))
                return false

        cancelExpectation(resources)

        list@ for ((resource, quantity) in resources) {
            resource as ItemType
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
                        continue@list
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
                    continue@list
                }
            }
        }
        listeners.forEach { it.onAddToContainer(this, resources) }
        return true
    }

    override fun expect(resources: ResourceList): Boolean {
        if (resources.keys.any { !isRightType(it) })
            return false
        if (spaceFor(expected + resources)) {
            expected.addAll(resources)
            return true
        }
        return false
    }

    override fun cancelExpectation(resources: ResourceList): Boolean {
        val ret = expected.takeAll(resources)
        return ret
    }

    override fun getSpaceForType(type: ResourceType): Int {
        return 0
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
        return add(i.type, i.quantity)
    }

    override fun spaceFor(list: ResourceList): Boolean {
        var extraSlots = items.count { it == null }
        for ((resource, quantity) in list) {
            resource as ItemType
            // the extra space on existing stacks of this type
            val extraSpace = items.filter { it?.type == resource && it.quantity != resource.maxStack }.sumBy { resource.maxStack - it!!.quantity }
            if (extraSpace < quantity) {
                // if we can't fit all we want to add in existing stacks
                // we need to use up a new slot for each stack we want to add
                extraSlots -= Math.ceil((quantity - extraSpace) / resource.maxStack.toDouble()).toInt()
                if(extraSlots < 0) {
                    return false
                }
            }
        }
        return true
    }

    override fun remove(resources: ResourceList, to: ResourceNode?, checkIfAble: Boolean): Boolean {
        if (checkIfAble)
            if (!canRemove(resources))
                return false
        list@ for ((resource, quantity) in resources) {
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
                        continue@list
                    }
                }
            }
        }
        listeners.forEach { it.onRemoveFromContainer(this, resources) }
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

    override fun toResourceList(): ResourceList {
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

    override fun equals(other: Any?): Boolean {
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

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + items.contentHashCode()
        result = 31 * result + expected.hashCode()
        result = 31 * result + totalQuantity
        return result
    }
}