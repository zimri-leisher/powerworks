package player

import data.ConcurrentlyModifiableWeakMutableList
import data.FileManager
import data.GameDirectoryIdentifier
import item.ItemType
import level.ActualLevel
import level.LevelManager
import level.entity.robot.BrainRobot
import main.Game
import network.ClientNetworkManager
import network.ServerNetworkManager
import network.User
import network.packet.*
import java.util.*

object PlayerManager : PacketHandler {
    lateinit var localPlayer: Player

    val allPlayers = ConcurrentlyModifiableWeakMutableList<Player>()

    val actionsAwaitingAck = mutableListOf<PlayerActionPacket>()

    fun isLocalPlayerLoaded() = ::localPlayer.isInitialized

    init {
        ServerNetworkManager.registerClientPacketHandler(this, PacketType.PLAYER_ACTION)
        ClientNetworkManager.registerServerPacketHandler(this, PacketType.ACK_PLAYER_ACTION)
    }

    fun getPlayer(user: User): Player {
        var alreadyExistingPlayer: Player? = null
        allPlayers.forEach {
            if (it.user == user) {
                alreadyExistingPlayer = it
            }
        }
        if (alreadyExistingPlayer != null) {
            // player has already been loaded this server session
            return alreadyExistingPlayer!!
        } else {
            var player = tryLoadPlayer(user.id)
            if (player == null) {
                // player has never connected before
                // will create and save a new level
                player = newPlayer(user)
            } else {
                // player has connected before but not this session
                // preload the players level if it is not already
                if (LevelManager.allLevels.none { it.id == player.homeLevelId }) {
                    ActualLevel(player.homeLevelId, LevelManager.tryLoadLevelInfo(player.homeLevelId)!!)
                }
            }
            return player
        }
    }

    fun tryLoadPlayer(id: UUID) = FileManager.tryLoadObject(GameDirectoryIdentifier.PLAYERS, "$id.player", Player::class.java)

    fun newPlayer(forUser: User): Player {
        val level = ActualLevel(LevelManager.newLevelId(), LevelManager.newLevelInfoFor(forUser))
        val player = Player(forUser, level.id, UUID.randomUUID())
        val brainRobot = BrainRobot(level.widthPixels / 2, level.heightPixels / 2, 0, player.user)
        for (type in ItemType.ALL) {
            brainRobot.inventory.add(type, type.maxStack)
        }
        level.add(brainRobot)
        player.brainRobotId = brainRobot.id
        LevelManager.saveLevelData(level.id, level.data)
        LevelManager.saveLevelInfo(level.id, level.info)
        return player
    }

    fun savePlayer(player: Player) = FileManager.saveObject(GameDirectoryIdentifier.PLAYERS, "${player.user.id}.player", player)

    fun savePlayers() {
        allPlayers.forEach { savePlayer(it) }
    }

    fun takeAction(action: PlayerAction) {
        if (Game.IS_SERVER) {
            if (action.verify()) {
                action.act()
            }
        } else {
            val packet = PlayerActionPacket(action)
            actionsAwaitingAck.add(packet)
            ClientNetworkManager.sendToServer(packet)
            action.actGhost()
        }
    }

    override fun handleClientPacket(packet: Packet) {
        if (packet.type != PacketType.PLAYER_ACTION)
            return
        packet as PlayerActionPacket
        var ownerShouldBe: Player? = null
        allPlayers.forEach {
            if (it.user == packet.fromUser) {
                ownerShouldBe = it
            }
        }
        if (ownerShouldBe == null) {
            println("Received an action from a user whose player has not been loaded yet")
            return
        }
        if (ownerShouldBe != packet.action.owner) {
            println("Received an action but the owner was incorrect--suspicious client")
            return
        }
        if (!packet.action.verify()) {
            ServerNetworkManager.sendToClient(AcknowledgePlayerActionPacket(packet.id, false), packet.connectionId)
        } else {
            ServerNetworkManager.sendToClient(AcknowledgePlayerActionPacket(packet.id, true), packet.connectionId)
            if (!packet.action.act()) {
                println("Incongruity between verify and act requirements")
            }
        }
    }

    override fun handleServerPacket(packet: Packet) {
        if (packet.type == PacketType.ACK_PLAYER_ACTION) {
            packet as AcknowledgePlayerActionPacket
            val ackdPacket = actionsAwaitingAck.firstOrNull { it.id == packet.ackPacketId }
            if (ackdPacket == null) {
                println("Received acknowledgement for a packet that wasn't being waited on")
            } else {
                ackdPacket.action.cancelActGhost()
                actionsAwaitingAck.remove(ackdPacket)
                if (!packet.success) {
                    println("Server denied ${ackdPacket.action}")
                }
            }
        }
    }
}