package network.packet

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag
import main.Version
import network.User
import java.util.*

class ClientHandshakePacket(
        @Tag(3)
        val timestamp: Long,
        @Tag(4)
        val newUser: User,
        @Tag(5)
        val version: Version) : Packet(PacketType.CLIENT_HANDSHAKE) {

    private constructor() : this(0, User(UUID.randomUUID(), ""), Version.`0`)
}