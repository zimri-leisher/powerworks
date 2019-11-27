package network.packet

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag

class ServerHandshakePacket(
        @Tag(3)
        val clientTimestamp: Long,
        @Tag(4)
        val serverTimestamp: Long,
        @Tag(5)
        val accepted: Boolean) : Packet(PacketType.SERVER_HANDSHAKE) {

    private constructor() : this(0, 0, false)
}