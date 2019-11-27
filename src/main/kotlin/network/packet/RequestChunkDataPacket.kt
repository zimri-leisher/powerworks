package network.packet

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag
import level.LevelInfo
import level.generator.LevelType
import network.User
import java.util.*

class RequestChunkDataPacket(
        @Tag(2)
        val levelId: UUID,
        @Tag(3)
        val xChunk: Int,
        @Tag(4)
        val yChunk: Int) : Packet(PacketType.REQUEST_CHUNK_DATA) {
    private constructor() : this(UUID.randomUUID(), 0, 0)
}