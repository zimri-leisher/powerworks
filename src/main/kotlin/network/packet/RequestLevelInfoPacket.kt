package network.packet

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer
import network.User
import java.util.*

class RequestLevelInfoPacket(
        @TaggedFieldSerializer.Tag(3)
        val levelId: UUID) : Packet(PacketType.REQUEST_LEVEL_INFO) {
    private constructor() : this(UUID.randomUUID())
}