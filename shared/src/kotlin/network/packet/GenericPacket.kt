package network.packet

import level.LevelManager
import resource.ResourceCategory
import resource.ResourceNodeOld
import resource.ResourceNodeBehavior
import resource.SourceContainer
import serialization.Id

class GenericPacket(
        @Id(2)
        val message: ResourceNodeBehavior
) : Packet(PacketType.GENERIC) {

    private constructor() : this(ResourceNodeBehavior(ResourceNodeOld(0, 0, 0, ResourceCategory.ITEM, SourceContainer(), LevelManager.EMPTY_LEVEL)))
}