package item

import resource.*

class Inventory(val width: Int, val height: Int, rule: (ResourceType) -> Boolean = { true }, private val items: Array<Item?> = arrayOfNulls(width * height)) : ResourceContainer<ItemType>(ResourceCategory.ITEM, rule) {

    var itemCount = 0
        private set

    val full: Boolean
        get() {
            if (items[items.lastIndex] != null) {
                return items.any { it!!.quantity != it.type.maxStack }
            }
            return false
        }

    override fun add(resource: ResourceType, quantity: Int, from: ResourceNode<*>?, checkIfAble: Boolean): Boolean {
        if (checkIfAble)
            if (!canAdd(resource, quantity))
                return false
        resource as ItemType
        var amountLeftToAdd = quantity
        itemCount += quantity
        // fill out unmaxed stacks
        for (item in items) {
            if (item != null && item.type == resource && item.quantity < resource.maxStack) {
                if(amountLeftToAdd + item.quantity > resource.maxStack) {
                    amountLeftToAdd -= resource.maxStack - item.quantity
                    item.quantity = resource.maxStack
                } else {
                    item.quantity += amountLeftToAdd
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
                listeners.forEach { it.onContainerChange(this, resource, quantity) }
                return true
            }
        }
        throw Exception("Inventory unable to accept more, use the checkIfAble argument when calling this")
    }

    override fun getQuantity(resource: ResourceType): Int {
        var q = 0
        for (i in items) {
            if (i != null && i.type == resource)
                q += i.quantity
        }
        return q
    }

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

    override fun spaceFor(resource: ItemType, quantity: Int): Boolean {
        var capacity = 0
        for (i in items.indices) {
            val item = items[i]
            if (item != null) {
                if (item.type == resource) {
                    capacity += resource.maxStack - item.quantity
                }
            } else {
                capacity += resource.maxStack
            }
        }
        return capacity >= quantity
    }

    override fun remove(resource: ResourceType, quantity: Int, to: ResourceNode<*>?, checkIfAble: Boolean): Boolean {
        if (checkIfAble)
            if (!canRemove(resource, quantity))
                return false
        var amountLeftToRemove = quantity
        itemCount -= quantity
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
                    listeners.forEach { it.onContainerChange(this, resource, -quantity) }
                    return true
                }
            }
        }
        throw Exception("Inventory does not contain enough resources, use the checkIfAble argument when calling this")
    }

    fun remove(i: Item): Boolean {
        return remove(i.type, i.quantity)
    }

    override fun contains(resource: ItemType, quantity: Int): Boolean {
        var contains = 0
        for (i in items.indices) {
            if (items[i] != null) {
                val item = items[i]!!
                if (item.type == resource) {
                    contains += item.quantity
                }
            }
        }
        return contains >= quantity
    }

    override fun toList(): ResourceList {
        val map = mutableMapOf<ResourceType, Int>()
        for(item in items) {
            if(item != null) {
                if(item.type in map) {
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

    fun print() {
        for (y in 0 until height) {
            for (x in 0 until width)
                print("${items[x + y * width]?.type?.name}      ")
            println()
        }
    }

    override fun clear() {
        for (i in items.indices) {
            items[i] = null
        }
        listeners.forEach { it.onContainerClear(this) }
    }

    override fun copy() = Inventory(width, height, typeRule, items.copyOf()).apply { this@apply.additionRule = this@Inventory.additionRule; this@apply.removalRule = this@Inventory.removalRule }

    operator fun iterator(): Iterator<Item?> {
        return items.iterator()
    }

    operator fun get(i: Int): Item? {
        return items[i]
    }

    operator fun set(i: Int, v: Item?) {
        items[i] = v
    }

    override fun toString() = "Inventory width: $width, height: $height, $itemCount"
}
