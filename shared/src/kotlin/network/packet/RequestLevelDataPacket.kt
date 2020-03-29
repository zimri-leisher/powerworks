package network.packet

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer
import level.LevelInfo
import level.generator.LevelType
import network.User
import serialization.Id
import java.util.*

class RequestLevelDataPacket(
        @Id(3)
        val levelId: UUID) : Packet(PacketType.REQUEST_LEVEL_DATA) {
    private constructor() : this(UUID.randomUUID())
}