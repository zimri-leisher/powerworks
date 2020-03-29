package network.packet

import serialization.Id
import java.util.*

class AcknowledgeLevelModificationPacket(
        @Id(4)
        val ackPacketLevelId: UUID,
        @Id(2)
        val ackPacketId: UUID,
        @Id(3)
        val success: Boolean) : Packet(PacketType.ACK_LEVEL_MODIFICATION) {
    private constructor() : this(UUID.randomUUID(), UUID.randomUUID(), false)
}