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

    /**
     * Gets the [Player] that corresponds to the given user. If the same player has already been loaded this server session,
     * returns that instance. Otherwise, if the player has connected before but not on this server session, loads them and their
     * home level from the disk. Otherwise, creates a new player and saves it to the disk, and creates a new [Level] for them and saves that to
     * the disk too.
     */
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

    private fun tryLoadPlayer(id: UUID) = FileManager.tryLoadObject(GameDirectoryIdentifier.PLAYERS, "$id.player", Player::class.java)

    private fun newPlayer(forUser: User): Player {
        val level = ActualLevel(LevelManager.newLevelId(), LevelManager.newLevelInfoFor(forUser))
        val player = Player(forUser, level.id, UUID.nameUUIDFromBytes(ByteArray(1)))
        val brainRobot = BrainRobot(level.widthPixels / 2, level.heightPixels / 2, 0, player.user)
        brainRobot.team = player.team
        for (type in ItemType.ALL) {
            brainRobot.inventory.add(type, type.maxStack)
        }
        level.add(brainRobot)
        brainRobot.id = player.brainRobotId
        LevelManager.saveLevelData(level.id, level.data)
        LevelManager.saveLevelInfo(level.id, level.info)
        return player
    }

    private fun savePlayer(player: Player) = FileManager.saveObject(GameDirectoryIdentifier.PLAYERS, "${player.user.id}.player", player)

    fun savePlayers() {
        allPlayers.forEach { savePlayer(it) }
    }

    /**
     * Tries to take an action as the [Player] specified in the [PlayerAction.owner].
     * To check if the action is possible, first, [PlayerAction.verify] is called. If it returns `true` and this is the
     * server, it will act immediately. Note that taking a [PlayerAction] on the server side has no guarantee of acting on
     * the client side. The way to communicate [PlayerAction]s to the client is to make sure that the [PlayerAction]s
     * cause [level.LevelUpdate]s, which _will_ get sent to the client.
     *
     * Alternatively, if this is the client and the action is successfully verified, it will send a [PlayerActionPacket]
     * to the server, add the packet to the [actionsAwaitingAck], and call [PlayerAction.actGhost]
     */
    fun takeAction(action: PlayerAction) {
        if (!action.verify()) {
            return
        }
        if (Game.IS_SERVER) {
            action.act()
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