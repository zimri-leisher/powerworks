package inv

import level.node.StorageNode
import level.resource.ResourceType
import java.util.*

class Inventory(val width: Int, val height: Int) : StorageNode<ItemType>(ResourceType.ITEM) {

    private val items = arrayOfNulls<Item>(width * height)

    val full: Boolean
        get() {
            if (items[items.lastIndex] != null) {
                return items.any { it!!.quantity != it.type.maxStack }
            }
            return false
        }

    fun add(item: Item): Boolean {
        if (full)
            return false
        var lowestIndex = items.lastIndex
        for (x in items.indices) {
            val i = items[x]
            if (i != null)
                if (i.type == item.type && i.quantity < i.type.maxStack) {
                    if (i.quantity + item.quantity > i.type.maxStack) {
                        item.quantity -= i.type.maxStack - i.quantity
                        i.quantity = i.type.maxStack
                        if (x < lowestIndex)
                            lowestIndex = x
                    } else {
                        i.quantity += item.quantity
                        if (x < lowestIndex)
                            lowestIndex = x
                        sort(lowestIndex, items.lastIndex)
                        return true
                    }
                }
        }
        for (i in 0 until items.lastIndex) {
            if (items[i] == null) {
                if (item.quantity > item.type.maxStack) {
                    items[i] = Item(item.type, item.type.maxStack)
                    item.quantity -= item.type.maxStack
                } else {
                    items[i] = Item(item.type, item.quantity)
                    sort()
                    return true
                }
            }
        }
        return false
    }

    override fun remove(type: ItemType, quantity: Int): Boolean {
        var amountLeft = quantity
        var lowestIndex = items.lastIndex
        for (x in items.lastIndex downTo 0) {
            val i = items[x]
            if (i != null) {
                if (i.type == type) {
                    if (i.quantity > amountLeft) {
                        i.quantity -= amountLeft
                        return true
                    } else {
                        amountLeft -= i.quantity
                        items[x] = null
                        if (x < lowestIndex)
                            lowestIndex = x
                    }
                }
            }
        }
        sort(lowestIndex, items.lastIndex)
        return false
    }

    override fun spaceFor(resource: ItemType, quantity: Int): Boolean {
        var amountLeft = quantity
        if (full) return false
        items.forEach { item ->
            if (item != null) {
                if (item.type == resource) {
                    if (item.quantity != resource.maxStack) {
                        amountLeft -= (resource.maxStack - item.quantity)
                    }
                }
            } else {
                amountLeft -= (resource.maxStack)
            }
        }
        return amountLeft <= 0
    }

    override fun contains(resource: ItemType, quantity: Int): Boolean {
        var amountLeft = quantity
        items.forEach { item ->
            if (item != null && item.type == resource) {
                amountLeft -= item.quantity
                if (amountLeft <= 0)
                    return true
            }
        }
        return false
    }

    override fun add(type: ItemType, quantity: Int): Boolean {
        return add(Item(type, quantity))
    }

    private fun sort(sortStart: Int, sortEnd: Int) {
        if (sortStart == sortEnd)
            return
        Arrays.sort(items.sliceArray(sortStart until sortEnd), { o1, o2 -> if (o1 == null) 1 else if (o2 == null) -1 else o1.type.id.compareTo(o2.type.id) })
    }

    private fun sort() {
        Arrays.sort(items, { o1, o2 -> if (o1 == null) 1 else if (o2 == null) -1 else if (o1.type.id == o2.type.id) o2.quantity.compareTo(o1.quantity) else o1.type.id.compareTo(o2.type.id) })
    }

    operator fun iterator(): Iterator<Item?> {
        return items.iterator()
    }

    operator fun get(i: Int): Item? {
        return items[i]
    }

    operator fun set(i: Int, v: Item?) {
        items[i] = v
    }
}
