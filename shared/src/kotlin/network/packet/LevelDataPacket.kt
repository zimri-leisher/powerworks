package network.packet

import data.ConcurrentlyModifiableMutableList
import level.LevelData
import serialization.Id
import java.util.*

class LevelDataPacket(
        @Id(3)
        val levelId: UUID,
        @Id(4)
        val data: LevelData,
        @Id(5)
        val updatesCount: Int) : Packet(PacketType.LEVEL_DATA) {

    private constructor() : this(UUID.randomUUID(), LevelData(ConcurrentlyModifiableMutableList(), ConcurrentlyModifiableMutableList(), arrayOf(), mutableListOf()), 0)
}