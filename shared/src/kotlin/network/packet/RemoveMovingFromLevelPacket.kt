package network.packet

import network.User
import player.Player
import serialization.Id
import java.util.*

class RemoveMovingFromLevelPacket(
        @Id(3)
        val xPixel: Int,
        @Id(2)
        val yPixel: Int,
        @Id(4)
        val movingId: UUID,
        @Id(5)
        val levelId: UUID,
        @Id(6)
        val owner: Player) : Packet(PacketType.REMOVE_MOVING_FROM_LEVEL) {
    private constructor() : this(0, 0, UUID.randomUUID(), UUID.randomUUID(), Player(User(UUID.randomUUID(), ""), UUID.randomUUID(), UUID.randomUUID()))
}