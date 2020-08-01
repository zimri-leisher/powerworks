package player.lobby

import level.LevelManager
import network.ServerNetworkManager
import network.packet.LevelLoadedSuccessPacket
import network.packet.Packet
import network.packet.PacketHandler
import network.packet.PacketType
import player.Player
import player.PlayerManager
import java.util.*

class Lobby : PacketHandler {

    val players = mutableSetOf<Player>()

    val loadedLevelIds = mutableSetOf<UUID>()

    init {
        LobbyManager.allLobbies.add(this)
        ServerNetworkManager.registerClientPacketHandler(this, PacketType.LEVEL_LOADED_SUCCESS)
    }

    fun connectPlayer(player: Player) {
        println("connecting player to $this")
        players.add(player)
    }

    fun disconnectPlayer(player: Player) {
        println("disconnecting player $player from $this")
        players.remove(player)
        if (players.isEmpty()) {
            println("no more players left")
            loadedLevelIds.forEach { id -> LevelManager.allLevels.firstOrNull { it.id == id }?.paused = true }
            loadedLevelIds.clear()
        }
    }

    fun sendPacket(packet: Packet) {
        ServerNetworkManager.sendToPlayers(packet, players)
    }

    fun merge(other: Lobby): Lobby {
        val new = Lobby()
        players.forEach { new.connectPlayer(it) }
        other.players.forEach { new.connectPlayer(it) }
        return new
    }

    override fun handleClientPacket(packet: Packet) {
        if (packet is LevelLoadedSuccessPacket) {
            val player = PlayerManager.getPlayer(packet.fromUser)
            if (player in players) {
                loadedLevelIds.add(packet.levelId)
                LevelManager.allLevels.firstOrNull { it.id == packet.levelId }?.paused = false
            }
        }
    }

    override fun handleServerPacket(packet: Packet) {
    }
}