package network.packet

import level.LevelInfo
import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag
import level.generator.LevelType
import network.User
import serialization.Id
import java.util.*

class LevelInfoPacket(
        @Id(4)
        val levelId: UUID,
        @Id(3)
        val info: LevelInfo) : Packet(PacketType.LEVEL_INFO) {
    private constructor() : this(UUID.randomUUID(), LevelInfo(User(UUID.randomUUID(), ""), "", "", LevelType.EMPTY, 0))
}