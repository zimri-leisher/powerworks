package network.packet

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag
import main.Version
import network.User
import serialization.Id
import java.util.*

class ClientHandshakePacket(
        @Id(3)
        val timestamp: Long,
        @Id(4)
        val newUser: User,
        @Id(5)
        val version: Version) : Packet(PacketType.CLIENT_HANDSHAKE) {

    private constructor() : this(0, User(UUID.randomUUID(), ""), Version.`0`)
}