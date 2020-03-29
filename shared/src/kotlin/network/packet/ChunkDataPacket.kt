package network.packet

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag
import level.ChunkData
import level.LevelInfo
import level.generator.LevelType
import network.User
import serialization.Id
import java.util.*

class ChunkDataPacket(
        @Id(2)
        val levelId: UUID,
        @Id(3)
        val xChunk: Int,
        @Id(4)
        val yChunk: Int,
        @Id(5)
        val chunkData: ChunkData) : Packet(PacketType.CHUNK_DATA) {

    private constructor() : this(UUID.randomUUID(), 0, 0, ChunkData())
}