package network.packet

import level.update.DefaultLevelUpdate
import level.Level
import level.LevelManager
import level.update.LevelUpdate
import serialization.AsReference
import serialization.Id

class LevelUpdatePacket(
    @Id(4)
    val update: LevelUpdate,
) : Packet(PacketType.LEVEL_UPDATE) {
    private constructor() : this(DefaultLevelUpdate())

    override fun toString(): String {
        return "LevelUpdatePacket(update=$update)"
    }
}