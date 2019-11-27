package network.packet

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag
import level.ChunkData
import level.LevelInfo
import level.generator.LevelType
import network.User
import java.util.*

class ChunkDataPacket(
        @Tag(2)
        val levelId: UUID,
        @Tag(3)
        val xChunk: Int,
        @Tag(4)
        val yChunk: Int,
        @Tag(5)
        val chunkData: ChunkData) : Packet(PacketType.CHUNK_DATA) {

    private constructor() : this(UUID.randomUUID(), 0, 0, ChunkData())
}