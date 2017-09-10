package inv

import level.block.BlockTypes

class Item(val type: ItemType, var quantity: Int = 1) {
    val isPlaceable
        get() = type.placedBlock != BlockTypes.ERROR

    override fun toString(): String {
        return "Item stack, type: $type, quantity: $quantity"
    }
}