package network.packet

import level.DefaultLevelModification
import level.Level
import level.LevelManager
import level.LevelModification
import serialization.Id

sealed class ServerUpdatePacket(packetType: PacketType) : Packet(packetType)

sealed class LevelUpdatePacket(
        @Id(2)
        val level: Level) : ServerUpdatePacket(PacketType.LEVEL_UPDATE) {
    @Id(3)
    val levelTimeWhenSent = level.updatesCount
}

class LevelModificationPacket(@Id(4) val modification: LevelModification, level: Level) : LevelUpdatePacket(level) {
    private constructor() : this(DefaultLevelModification(), LevelManager.EMPTY_LEVEL)
}