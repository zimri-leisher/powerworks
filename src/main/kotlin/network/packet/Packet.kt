package network.packet

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag
import network.ServerNetworkManager

open class Packet(
        @Tag(1)
        val type: PacketType) {

    private constructor() : this(PacketType.UNKNOWN)

    var connectionId = 0

    lateinit var onSend: Packet.() -> Unit

    val connection get() = ServerNetworkManager.getConnection(connectionId)

    val fromUser get() = ServerNetworkManager.getUser(connectionId)
}