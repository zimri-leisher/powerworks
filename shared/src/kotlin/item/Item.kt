package item

import serialization.Id

open class Item(
        @Id(1)
        val type: ItemType = ItemType.ERROR,
        @Id(2)
        var quantity: Int = 1) {

    override fun toString(): String {
        return "Item stack, type: $type, quantity: $quantity"
    }
}