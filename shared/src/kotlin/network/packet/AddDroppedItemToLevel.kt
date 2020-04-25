package network.packet

import item.ItemType
import serialization.Id
import java.util.*

class AddDroppedItemToLevel(
        @Id(2)
        val itemType: ItemType,
        @Id(3)
        val quantity: Int,
        @Id(4)
        val xPixel: Int,
        @Id(5)
        val yPixel: Int,
        @Id(6)
        val levelId: UUID) : Packet(PacketType.ADD_DROPPED_ITEM_TO_LEVEL) {
    private constructor() : this(ItemType.ERROR, 0, 0, 0, UUID.randomUUID())
}