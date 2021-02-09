package player.lobby

import level.Level
import level.LevelEvent
import level.LevelEventListener
import level.LevelManager
import network.ServerNetworkManager
import network.packet.LevelLoadedSuccessPacket
import network.packet.Packet
import player.Player
import player.PlayerEvent
import player.PlayerEventListener
import player.PlayerManager
import java.util.*

class Lobby : PlayerEventListener, LevelEventListener {

    val players = mutableSetOf<Player>()

    val loadedLevels = mutableSetOf<Level>()

    init {
        PlayerManager.playerEventListeners.add(this)
        LevelManager.levelEventListeners.add(this)
    }

    fun connectPlayer(player: Player) {
        players.add(player)
        val homeLevel = LevelManager.getLevelByIdOrNull(player.homeLevelId)
        if(homeLevel != null && player.playing) {
            loadedLevels.add(homeLevel)
            homeLevel.paused = false
        }
    }

    fun disconnectPlayer(player: Player) {
        players.remove(player)
    }

    fun sendPacket(packet: Packet, players: Set<Player> = this.players) {
        for (player in players) {
            val connection = ServerNetworkManager.getConnectionIdByUserOrNull(player.user)
            if(connection != null) {
                ServerNetworkManager.sendToClient(packet, connection)
            } else {
                println("Tried to send a packet to user ${player.user}, but they were not connected")
            }
        }
    }

    fun merge(other: Lobby): Lobby {
        val new = Lobby()
        players.forEach { new.connectPlayer(it) }
        other.players.forEach { new.connectPlayer(it) }
        return new
    }

    override fun onLevelEvent(level: Level, event: LevelEvent) {
        if(event == LevelEvent.LOAD) {
            println("loaded level $level, current players in this lobby: $players")
            val player = players.firstOrNull { it.homeLevelId == level.id }
            if (player in players) {
                loadedLevels.add(level)
                level.paused = false
            }
        }
    }

    override fun onPlayerEvent(player: Player, event: PlayerEvent) {
        if (player in players) {
            if(event == PlayerEvent.STOP_PLAYING) {
                println("player $player stop playing. there are now ${players.filter { it.playing }}")
                if (players.none { it.playing }) {
                    println("there are no players playing")
                    println("pausing level $loadedLevels")
                    loadedLevels.forEach { it.paused = true }
                    loadedLevels.clear()
                }
            } else if(event == PlayerEvent.DISCONNECT) {
                disconnectPlayer(player)
            }
        }
    }
}