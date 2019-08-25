package item

open class Item(val type: ItemType, var quantity: Int = 1) {
    override fun toString(): String {
        return "Item stack, type: $type, quantity: $quantity"
    }
}