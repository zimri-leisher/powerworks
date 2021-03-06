package network.packet

import item.Inventory
import level.LevelManager
import resource.ResourceCategory
import resource.ResourceNode
import resource.ResourceNodeBehavior
import serialization.Id

class GenericPacket(
        @Id(2)
        val message: ResourceNodeBehavior
) : Packet(PacketType.GENERIC) {

    private constructor() : this(ResourceNodeBehavior(ResourceNode(0, 0, 0, ResourceCategory.ITEM, Inventory(1, 1), LevelManager.EMPTY_LEVEL)))
}