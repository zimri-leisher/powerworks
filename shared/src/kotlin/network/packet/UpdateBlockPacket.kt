package network.packet

import level.block.Block
import level.block.BlockType
import level.block.DefaultBlock
import serialization.Id

class UpdateBlockPacket(
        @Id(2)
        val block: Block) : Packet(PacketType.UPDATE_BLOCK) {
    private constructor() : this(DefaultBlock(BlockType.ERROR, 0, 0))
}