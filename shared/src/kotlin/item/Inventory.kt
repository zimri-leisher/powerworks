package item

import resource.*
import serialization.Id
import java.lang.Integer.min

class Inventory(
    @Id(11)
    val width: Int,
    @Id(7)
    val height: Int
) : ResourceContainer() {

    private constructor() : this(0, 0)

    val size get() = width * height

    val resources = mutableResourceListOf()

    @Id(10)
    override var totalQuantity = 0
        private set

    override fun add(resources: ResourceList): Boolean {
        if (!canAdd(resources))
            return false

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
            if (existingStackAmount > 0 && type.maxStack - existingStackAmount >= quantity) {
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

    override fun remove(resources: ResourceList): Boolean {
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

    override fun mostPossibleToRemove(list: ResourceList): ResourceList {
        val possible = mutableResourceListOf()
        for ((type, quantity) in list) {
            val existingQuantityOfType = items.sumBy { if (it != null && it.type == type) it.quantity else 0 }
            possible.put(type, min(quantity, existingQuantityOfType))
        }
        return possible
    }

    override fun getQuantity(type: ResourceType) = resources[type]

    override fun toResourceList() = resources

    override fun clear() {
        resources.clear()
        listeners.forEach { it.onContainerClear(this) }
    }

    override fun copy(): Inventory {
        val inv = Inventory(width, height)
        inv.resources.putAll(resources)
        return inv
    }

    operator fun iterator() = resources.iterator()

    operator fun get(i: Int):

    operator fun set(i: Int, v: Item?) {
        if (items[i] != null)
            totalQuantity -= items[i]!!.quantity
        items[i] = v
        totalQuantity += v?.quantity ?: 0
    }

    override fun toString() = "Inventory width: $width, height: $height, $totalQuantity items"

}