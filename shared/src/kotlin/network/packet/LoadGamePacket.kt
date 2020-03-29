package network.packet

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag
import level.LevelInfo
import level.generator.LevelType
import network.User
import player.Player
import resource.ResourceContainer
import resource.ResourceType
import serialization.Id
import java.util.*

class LoadGamePacket(
        @Id(2)
        val localPlayer: Player,
        @Id(3)
        val currentLevelInfo: LevelInfo) : Packet(PacketType.LOAD_GAME) {
    private constructor() : this(Player(User(UUID.randomUUID(), ""), UUID.randomUUID(), UUID.randomUUID()), LevelInfo(User(UUID.randomUUID(), ""), "", "", LevelType.EMPTY, 0))
}

class RequestLoadGamePacket(
        @Id(2)
        val forUser: User) : Packet(PacketType.REQUEST_LOAD_GAME) {
    private constructor() : this(User(UUID.randomUUID(), ""))
}