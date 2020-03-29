package network.packet

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag
import level.LevelManager
import level.block.Block
import level.block.BlockType
import level.block.DefaultBlock
import network.User
import player.Player
import serialization.Id
import java.util.*

// TODO for all LevelModifications, there should be an owner

class RemoveBlockFromLevelPacket(
        @Id(2)
        val xTile: Int,
        @Id(3)
        val yTile: Int,
        @Id(5)
        val levelId: UUID,
        @Id(4)
        val owner: Player) : Packet(PacketType.REMOVE_BLOCK) {
    private constructor() : this(0, 0, UUID.randomUUID(), Player(User(UUID.randomUUID(), ""), LevelManager.EMPTY_LEVEL.id, UUID.randomUUID()))
}