package network.packet

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag
import level.Level
import level.LevelManager
import level.block.BlockType
import java.util.*

class PlaceBlockPacket(
        @Tag(2)
        val blockType: BlockType<*>,
        @Tag(3)
        val xTile: Int,
        @Tag(4)
        val yTile: Int,
        @Tag(5)
        val levelId: UUID) : Packet(PacketType.PLACE_BLOCK) {

    private constructor() : this(BlockType.ERROR, 0, 0, UUID.randomUUID())
}