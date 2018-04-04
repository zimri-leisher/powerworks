package item

import resource.ResourceContainer
import resource.ResourceList
import resource.ResourceNode
import resource.ResourceType
import java.util.*

class InfiniteInventory(rule: (ResourceType) -> Boolean = { true }) : ResourceContainer<ItemType>(ResourceType.ITEM, rule) {

    var items = mutableListOf<Item>()
        private set

    override fun add(resource: ResourceType, quantity: Int, from: ResourceNode<*>?, checkIfAble: Boolean): Boolean {
        if(!isValid(resource))
            return false
        resource as ItemType
        if (checkIfAble)
            if (!spaceFor(resource, quantity) || !rule(resource))
                return false
        var amountLeftToAdd = quantity
        for (it in items) {
            if (it.type == resource) {
                val pQ = it.quantity
                it.quantity = Math.min(resource.maxStack, amountLeftToAdd + it.quantity)
                amountLeftToAdd -= it.quantity - pQ
                if (amountLeftToAdd <= 0) {
                    return true
                }
            }
        }
        while (amountLeftToAdd >= 0) {
            val q = Math.min(resource.maxStack, amountLeftToAdd)
            items.add(Item(resource, q))
            amountLeftToAdd -= q
        }
        return true
    }

    override fun spaceFor(resource: ResourceType, quantity: Int) = true

    override fun remove(resource: ResourceType, quantity: Int, to: ResourceNode<*>?, checkIfAble: Boolean): Boolean {
        if (checkIfAble)
            if (!contains(resource, quantity))
                return false
        var amountLeftToRemove = quantity
        val i = items.iterator()
        for (item in i) {
            if (item.type == resource) {
                val pQ = item.quantity
                item.quantity = Math.max(0, item.quantity - amountLeftToRemove)
                amountLeftToRemove -= (pQ - item.quantity)
                if (amountLeftToRemove <= 0) {
                    return true
                }
            }
        }
        throw Exception("Inventory does not contain enough resources, use the checkIfAble argument while calling this")
    }

    override fun contains(resource: ResourceType, quantity: Int) = items.any { it.type == resource && it.quantity >= quantity }

    override fun clear() {
        items.clear()
    }

    override fun copy(): ResourceContainer<ItemType> {
        return InfiniteInventory(rule).apply { Collections.copy(this.items, this@InfiniteInventory.items) }
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