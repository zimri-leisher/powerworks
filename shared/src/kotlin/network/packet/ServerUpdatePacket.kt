package network.packet

import level.update.DefaultLevelUpdate
import level.Level
import level.LevelManager
import level.update.LevelUpdate
import serialization.AsReference
import serialization.Id

sealed class ServerUpdatePacket(packetType: PacketType) : Packet(packetType)

class LevelUpdatePacket(
    @Id(4)
    val update: LevelUpdate,
    @Id(2)
    @AsReference
    val level: Level
) : ServerUpdatePacket(PacketType.LEVEL_UPDATE) {
    private constructor() : this(DefaultLevelUpdate(), LevelManager.EMPTY_LEVEL)

    override fun toString(): String {
        return "LevelUpdatePacket: (update=$update, level=$level)"
    }
}