package player

import data.ConcurrentlyModifiableWeakMutableList
import data.FileManager
import data.GameDirectoryIdentifier
import item.BlockItemType
import item.IngotItemType
import item.ItemType
import level.ActualLevel
import level.LevelManager
import level.entity.robot.BrainRobot
import main.Game
import main.PowerworksDelegates
import network.ClientNetworkManager
import network.ServerNetworkManager
import network.User
import network.packet.*
import resource.ResourceList
import resource.resourceListOf
import java.util.*

object PlayerManager : PacketHandler, PlayerEventListener {
    private val localPlayerDelegate = PowerworksDelegates.lateinitVal<Player>()
    var localPlayer: Player by localPlayerDelegate

    val allPlayers = ConcurrentlyModifiableWeakMutableList<Player>()

    val actionsAwaitingAck = mutableListOf<PlayerActionPacket>()

    val playerEventListeners = mutableListOf<PlayerEventListener>()

    private val startingInventory = resourceListOf(BlockItemType.ITEM_PIPE to 10, BlockItemType.MINER to 2, BlockItemType.SMELTER to 2, BlockItemType.CHEST_SMALL to 1,
            BlockItemType.CRAFTER to 1)

    fun isLocalPlayerLoaded() = localPlayerDelegate.initialized

    init {
        ServerNetworkManager.registerClientPacketHandler(this, PacketType.PLAYER_ACTION)
        ClientNetworkManager.registerServerPacketHandler(this, PacketType.ACK_PLAYER_ACTION)
        playerEventListeners.add(this)
    }

    fun pushPlayerEvent(player: Player, event: PlayerEvent) {
        playerEventListeners.forEach { it.onPlayerEvent(player, event) }
    }

    override fun onPlayerEvent(player: Player, event: PlayerEvent) {
        if (event == PlayerEvent.INITIALIZE) {
            allPlayers.add(player)
            val homeLevel = LevelManager.allLevels.firstOrNull { it.id == player.homeLevelId }
            if (homeLevel != null) {
                player.homeLevel = homeLevel
            }
            val brainRobot = LevelManager.loadedLevels.mapNotNull { it.data.brainRobots.firstOrNull { it.id == player.brainRobotId } }
            if (brainRobot.size > 1) {
                println("$player has ${brainRobot.size} brainrobots ERROR ERROR")
            } else if (brainRobot.size == 1) {
                player.brainRobot = brainRobot.first()
            }
        }
    }

    fun getInitializedPlayerByIdOrNull(id: UUID): Player? {
        var player: Player? = null
        allPlayers.forEach {
            if (it.user.id == id) {
                player = it
            }
        }
        return player
    }

    fun getOnlinePlayerOrNull(user: User): Player? {
        var player: Player? = null
        allPlayers.forEach {
            if (it.online && it.user == user) {
                player = it
            }
        }
        return player
    }

    fun getInitializedPlayerOrNull(user: User): Player? {
        var player: Player? = null
        allPlayers.forEach {
            if (it.user == user) {
                player = it
            }
        }
        return player
    }

    /**
     * Gets the [Player] that corresponds to the given user. If the same player has already been loaded this server session,
     * returns that instance. Otherwise, if the player has connected before but not on this server session, loads them and their
     * home level from the disk. Otherwise, creates a new player and saves it to the disk, and creates a new [Level] for them and saves that to
     * the disk too.
     */
    fun getPlayer(user: User): Player {
        return getInitializedPlayerOrNull(user)
                ?: tryLoadPlayerOrNull(user)
                        ?.also { player ->
                            println("trying to load player")
                            if (!LevelManager.isLevelInitialized(player.homeLevelId)) {
                                ActualLevel(player.homeLevelId, LevelManager.tryLoadLevelInfoFile(player.homeLevelId)!!).apply {
                                    initialize()
                                    load()
                                }
                            }
                        }
                ?: newFirstTimePlayer(user)
    }

    fun onUserDisconnect(user: User) {
        // make it realize it's no longer online
        getOnlinePlayerOrNull(user)?.online = false
    }

    private fun tryLoadPlayerOrNull(user: User) = FileManager.tryLoadObject(GameDirectoryIdentifier.PLAYERS, "${user.id}.player", Player::class.java)

    private fun newFirstTimePlayer(user: User): Player {
        val player = Player(user, LevelManager.newLevelId(), UUID.randomUUID())
        player.initialize()
        val level = ActualLevel(player.homeLevelId, LevelManager.newLevelInfoForFile(user))
        level.initialize()
        level.load()
        val brainRobot = BrainRobot(level.width / 2, level.height / 2, 2, player.user)
        player.brainRobot = brainRobot
        brainRobot.team = player.team
        brainRobot.inventory.add(startingInventory)
        level.add(brainRobot)
        brainRobot.id = player.brainRobotId
        LevelManager.saveLevelDataFile(level.id, level.data)
        LevelManager.saveLevelInfoFile(level.id, level.info)
        savePlayer(player)
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
        val ownerShouldBe = getInitializedPlayerOrNull(packet.fromUser)
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