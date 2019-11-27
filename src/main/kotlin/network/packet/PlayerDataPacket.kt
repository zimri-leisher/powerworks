package network.packet

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag
import level.LevelInfo
import level.entity.robot.BrainRobot
import level.generator.LevelType
import network.User
import java.util.*

class PlayerDataPacket(
        @Tag(2)
        val forUser: User,
        @Tag(3)
        val levelId: UUID,
        @Tag(5)
        val levelInfo: LevelInfo,
        @Tag(4)
        val brainRobot: BrainRobot) : Packet(PacketType.PLAYER_DATA) {

    private constructor() : this(User(UUID.randomUUID(), ""), UUID.randomUUID(), LevelInfo(User(UUID.randomUUID(), ""), "", "", LevelType.EMPTY, 0), BrainRobot(0, 0, 0, User(UUID.randomUUID(), "")))
}