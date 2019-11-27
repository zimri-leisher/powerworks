package network.packet

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag
import data.ConcurrentlyModifiableMutableList
import level.LevelData
import level.LevelInfo
import level.generator.LevelType
import network.User
import java.util.*

class LevelDataPacket(
        @Tag(3)
        val id: UUID,
        @Tag(4)
        val data: LevelData) : Packet(PacketType.LEVEL_DATA) {

    private constructor() : this(UUID.randomUUID(), LevelData(ConcurrentlyModifiableMutableList(), mutableListOf(), arrayOf()))
}