package player

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag
import level.Level
import level.entity.robot.BrainRobot
import network.User

class Player(
        @Tag(1)
        val user: User,
        @Tag(2)
        val homeLevel: Level,
        @Tag(3)
        val brainRobot: BrainRobot) {
}