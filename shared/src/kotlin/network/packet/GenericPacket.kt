package network.packet

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer
import item.Inventory
import level.Level
import level.LevelManager
import level.block.Block
import level.block.ChestBlock
import level.block.ChestBlockType
import resource.ResourceCategory
import resource.ResourceNode
import resource.ResourceNodeBehavior
import routing.TubeRoutingNetwork
import serialization.Id

class GenericPacket(
        @Id(2)
        val message: ResourceNodeBehavior
) : Packet(PacketType.GENERIC) {

    private constructor() : this(ResourceNodeBehavior(ResourceNode(0, 0, 0, ResourceCategory.ITEM, Inventory(1, 1), LevelManager.EMPTY_LEVEL)))
}