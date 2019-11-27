package player

import data.FileManager
import data.GameDirectoryIdentifier
import level.ActualLevel
import level.CHUNK_PIXEL_EXP
import level.LevelManager
import level.RemoteLevel
import level.entity.robot.BrainRobot
import main.Game
import network.ClientNetworkManager
import network.ServerNetworkManager
import network.packet.*
import java.util.*

object PlayerManager : PacketHandler {

    lateinit var localPlayer: Player
    val allPlayers = mutableListOf<Player>()

    init {
        ServerNetworkManager.registerClientPacketHandler(this, PacketType.REQUEST_PLAYER_DATA)
        ClientNetworkManager.registerServerPacketHandler(this, PacketType.PLAYER_DATA)
    }

    fun isLocalPlayerLoaded() = ::localPlayer.isInitialized

    override fun handleClientPacket(packet: Packet) {
        if (packet is RequestPlayerDataPacket) {
            var player = allPlayers.firstOrNull { it.user == packet.fromUser }
            if (player == null) {
                // player hasn't been loaded this server session
                player = FileManager.loadObject(GameDirectoryIdentifier.PLAYERS, "${packet.fromUser.id}.player", Player::class.java)
                if (player == null) {
                    // player has never joined before
                    val info = LevelManager.newLevelInfoFor(packet.fromUser)
                    player = Player(packet.fromUser, ActualLevel(LevelManager.newLevelId(), info), BrainRobot((info.levelType.widthChunks / 2) shl CHUNK_PIXEL_EXP, (info.levelType.heightChunks / 2) shl CHUNK_PIXEL_EXP , 0, packet.fromUser))
                    FileManager.saveObject(GameDirectoryIdentifier.PLAYERS, "${packet.fromUser.id}.player", player)
                }
                allPlayers.add(player)
            }
            ServerNetworkManager.sendToClient(PlayerDataPacket(player.user, player.homeLevel.id, player.homeLevel.info, player.brainRobot), packet.connectionId)
        }
    }

    override fun handleServerPacket(packet: Packet) {
        if (packet is PlayerDataPacket) {
            val level = RemoteLevel(packet.levelId, packet.levelInfo)
            val player = Player(packet.forUser, level, packet.brainRobot)
            if (packet.forUser == Game.USER && !isLocalPlayerLoaded()) {
                localPlayer = player
            }
            allPlayers.add(player)
        }
    }
}
