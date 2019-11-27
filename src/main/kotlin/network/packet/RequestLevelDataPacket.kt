package network.packet

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer
import level.LevelInfo
import level.generator.LevelType
import network.User
import java.util.*

class RequestLevelDataPacket(
        @TaggedFieldSerializer.Tag(3)
        val id: UUID) : Packet(PacketType.REQUEST_LEVEL_DATA) {
    private constructor() : this(UUID.randomUUID())
}