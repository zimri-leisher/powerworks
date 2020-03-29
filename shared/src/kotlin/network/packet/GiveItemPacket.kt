package network.packet

import item.ItemType
import java.util.*

class GiveItemPacket(val itemType: ItemType, val quantity: Int, val containerId: UUID) : Packet(PacketType.GIVE_ITEM) {
    private constructor() : this(ItemType.ERROR, 0, UUID.randomUUID())
}