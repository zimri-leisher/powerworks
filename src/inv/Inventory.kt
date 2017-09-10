package inv

import java.util.*

class Inventory(val width: Int, val height: Int) {
    private val items = arrayOfNulls<Item>(width * height)

    val full
        get() = items[items.lastIndex] != null

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

    fun remove(type: ItemType, quantity: Int = 1): Boolean {
        var amountLeft = quantity
        var lowestIndex = items.lastIndex
        for (x in items.lastIndex downTo 0) {
            val i = items[x]
            if(i != null) {
                if(i.type == type) {
                    if(i.quantity > amountLeft) {
                        i.quantity -= amountLeft
                        return true
                    } else {
                        amountLeft -= i.quantity
                        items[x] = null
                        if(x < lowestIndex)
                            lowestIndex = x
                    }
                }
            }
        }
        sort(lowestIndex, items.lastIndex)
        return false
    }

    fun add(type: ItemType, quantity: Int = 1): Boolean {
        return add(Item(type, quantity))
    }

    private fun sort(sortStart: Int, sortEnd: Int) {
        if(sortStart == sortEnd)
            return
        Arrays.sort(items.sliceArray(sortStart until sortEnd), { o1, o2 -> if (o1 == null) 1 else if (o2 == null) -1 else o1.type.id.compareTo(o2.type.id) })
    }

    private fun sort() {
        Arrays.sort(items, { o1, o2 -> if (o1 == null) 1 else if (o2 == null) -1 else o1.type.id.compareTo(o2.type.id) })
    }

    operator fun iterator(): Iterator<Item?> {
        return items.iterator()
    }

    operator fun get(i: Int): Item? {
        return items[i]
    }
}
