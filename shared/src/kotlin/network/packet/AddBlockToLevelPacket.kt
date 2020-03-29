package network.packet

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag
import level.Level
import level.LevelManager
import level.block.Block
import level.block.BlockType
import level.block.DefaultBlock
import network.User
import player.Player
import serialization.Id
import java.util.*

class AddBlockToLevelPacket(
        @Id(2)
        val blockType: BlockType<*>,
        @Id(3)
        val xTile: Int,
        @Id(4)
        val yTile: Int,
        @Id(5)
        val rotation: Int,
        @Id(6)
        val levelId: UUID,
        @Id(7)
        val owner: Player) : Packet(PacketType.ADD_BLOCK_TO_LEVEL) {

    private constructor() : this(BlockType.ERROR, 0, 0, 0, UUID.randomUUID(),
            Player(User(UUID.randomUUID(), ""), LevelManager.EMPTY_LEVEL.id, UUID.randomUUID()))
}