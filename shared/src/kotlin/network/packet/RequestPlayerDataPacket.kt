package network.packet

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer
import network.User
import serialization.Id
import java.util.*

class RequestPlayerDataPacket(
        @Id(2)
        val forUser: User) : Packet(PacketType.REQUEST_PLAYER_DATA) {
    private constructor() : this(User(UUID.randomUUID(), ""))
}