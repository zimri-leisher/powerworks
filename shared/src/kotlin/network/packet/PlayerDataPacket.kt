package network.packet

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag
import level.LevelInfo
import level.entity.robot.BrainRobot
import level.generator.LevelType
import network.User
import player.Player
import serialization.Id
import java.util.*

class PlayerDataPacket(
        @Id(2)
        val player: Player) : Packet(PacketType.PLAYER_DATA) {

    private constructor() : this(Player(User(UUID.randomUUID(), ""), UUID.randomUUID(), UUID.randomUUID()))
}