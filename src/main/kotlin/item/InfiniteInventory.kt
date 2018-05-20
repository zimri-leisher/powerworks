package item

import resource.*
import java.util.*

class InfiniteInventory(rule: (ResourceType) -> Boolean = { true }) : ResourceContainer<ItemType>(ResourceCategory.ITEM, rule) {

    var items = mutableListOf<Item>()
        private set

    override fun add(resource: ResourceType, quantity: Int, from: ResourceNode<*>?, checkIfAble: Boolean): Boolean {
        if (checkIfAble)
            if (!canAdd(resource, quantity))
                return false
        resource as ItemType
        var amountLeftToAdd = quantity
        for (it in items) {
            if (it.type == resource) {
                val pQ = it.quantity
                it.quantity = Math.min(resource.maxStack, amountLeftToAdd + it.quantity)
                amountLeftToAdd -= it.quantity - pQ
                if (amountLeftToAdd <= 0) {
                    listeners.forEach { it.onContainerChange(this, resource, quantity) }
                    return true
                }
            }
        }
        while (amountLeftToAdd >= 0) {
            val q = Math.min(resource.maxStack, amountLeftToAdd)
            items.add(Item(resource, q))
            amountLeftToAdd -= q
        }
        listeners.forEach { it.onContainerChange(this, resource, quantity) }
        return true
    }

    override fun spaceFor(resource: ItemType, quantity: Int) = true

    override fun remove(resource: ResourceType, quantity: Int, to: ResourceNode<*>?, checkIfAble: Boolean): Boolean {
        if (checkIfAble)
            if (!canRemove(resource, quantity))
                return false
        var amountLeftToRemove = quantity
        val i = items.iterator()
        for (item in i) {
            if (item.type == resource) {
                val pQ = item.quantity
                item.quantity = Math.max(0, item.quantity - amountLeftToRemove)
                amountLeftToRemove -= (pQ - item.quantity)
                if (amountLeftToRemove <= 0) {
                    listeners.forEach { it.onContainerChange(this, resource, -quantity) }
                    return true
                }
            }
        }
        throw Exception("Inventory does not contain enough resources, use the checkIfAble argument while calling this")
    }

    override fun contains(resource: ItemType, quantity: Int) = items.any { it.type == resource && it.quantity >= quantity }

    override fun clear() {
        items.clear()
        listeners.forEach { it.onContainerClear(this) }
    }

    override fun copy(): ResourceContainer<ItemType> {
        return InfiniteInventory(typeRule).apply { Collections.copy(this.items, this@InfiniteInventory.items) }
    }

    override fun getQuantity(resource: ResourceType): Int {
        var q = 0
        items.forEach { if (it.type == resource) q += it.quantity }
        return q
    }

    override fun toList(): ResourceList {
        val map = mutableMapOf<ResourceType, Int>()
        for (item in items) {
            if (item.type in map) {
                val newQ = map.get(item.type)!! + item.quantity
                map.replace(item.type, newQ)
            } else {
                map.put(item.type, item.quantity)
            }
        }
        return ResourceList(map)
    }

}