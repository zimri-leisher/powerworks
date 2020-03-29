package network.packet

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag
import serialization.Id

class ServerHandshakePacket(
        @Id(3)
        val clientTimestamp: Long,
        @Id(4)
        val serverTimestamp: Long,
        @Id(5)
        val accepted: Boolean) : Packet(PacketType.SERVER_HANDSHAKE) {

    private constructor() : this(0, 0, false)
}