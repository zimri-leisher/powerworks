package item

import resource.*
import serialization.Id
import java.lang.Integer.min

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
    override val expected = mutableResourceListOf()

    @Id(10)
    override var totalQuantity = 0
        private set

    override fun add(resources: ResourceList, from: ResourceNodeOld?, checkIfAble: Boolean): Boolean {
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

    override fun getQuantity(resource: ResourceType) = items.filter { it?.type == resource }.sumBy { it?.quantity ?: 0 }

    fun add(i: Item): Boolean {
        return add(i.type, i.quantity)
    }

    override fun mostPossibleToAdd(list: ResourceList): ResourceList {
        val possible = mutableResourceListOf()
        var extraSlots = items.count { it == null }
        for ((type, quantity) in list) {
            if (type !is ItemType) {
                continue
            }
            var existingStackAmount = 0
            for (item in items) {
                if (item != null && item.type == type && item.quantity < item.type.maxStack) {
                    existingStackAmount = item.quantity
                }
            }
            if (existingStackAmount > 0 && type.maxStack - existingStackAmount >= quantity ) {
                possible.put(type, quantity)
            } else {
                val extraStackAmount = type.maxStack - existingStackAmount
                val neededEmptyStacks = Math.ceil((quantity - extraStackAmount).toDouble() / type.maxStack).toInt()
                if (neededEmptyStacks <= extraSlots) {
                    possible.put(type, quantity)
                    extraSlots -= neededEmptyStacks
                } else {
                    possible.put(type, extraStackAmount + extraSlots * type.maxStack)
                    extraSlots = 0
                }
            }
        }
        return possible
    }

    override fun remove(resources: ResourceList, to: ResourceNodeOld?, checkIfAble: Boolean): Boolean {
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

    override fun mostPossibleToRemove(list: ResourceList): ResourceList {
        val possible = mutableResourceListOf()
        for ((type, quantity) in list) {
            val existingQuantityOfType = items.sumBy { if (it != null && it.type == type) it.quantity else 0 }
            possible.put(type, min(quantity, existingQuantityOfType))
        }
        return possible
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

}