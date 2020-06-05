package network.packet

import player.ErrorAction
import player.PlayerAction
import serialization.Id
import java.util.*

class PlayerActionPacket(
        @Id(2)
        val action: PlayerAction) : Packet(PacketType.PLAYER_ACTION) {
    private constructor() : this(ErrorAction())
}

class AcknowledgePlayerActionPacket(
        @Id(2)
        val ackPacketId: UUID,
        @Id(3)
        val success: Boolean) : Packet(PacketType.ACK_PLAYER_ACTION) {
    private constructor() : this(UUID.randomUUID(), false)
}