package network.packet

import serialization.Id
import java.util.*

class LevelLoadedSuccessPacket(
        @Id(2)
        val levelId: UUID) : Packet(PacketType.LEVEL_LOADED_SUCCESS) {
    private constructor() : this(UUID.randomUUID())
}