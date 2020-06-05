package network.packet

import main.Game
import network.ServerNetworkManager
import serialization.Id
import java.util.*

open class Packet(
        @Id(1)
        val type: PacketType) {

    private constructor() : this(PacketType.UNKNOWN)

    @Id(-1)
    val updateTime = Game.updatesCount

    @Id(0)
    val id = UUID.randomUUID()

    var connectionId = 0

    lateinit var onSend: Packet.() -> Unit

    val connection get() = ServerNetworkManager.getConnection(connectionId)

    val fromUser get() = ServerNetworkManager.getUser(connectionId)
}