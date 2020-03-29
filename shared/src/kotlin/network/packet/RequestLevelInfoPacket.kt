package network.packet

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer
import network.User
import serialization.Id
import java.util.*

class RequestLevelInfoPacket(
        @Id(3)
        val levelId: UUID) : Packet(PacketType.REQUEST_LEVEL_INFO) {
    private constructor() : this(UUID.randomUUID())
}