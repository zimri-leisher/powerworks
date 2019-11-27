package item

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag

open class Item(
        @Tag(1)
        val type: ItemType = ItemType.ERROR,
        @Tag(2)
        var quantity: Int = 1) {

    override fun toString(): String {
        return "Item stack, type: $type, quantity: $quantity"
    }
}