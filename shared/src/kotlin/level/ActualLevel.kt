package level

import level.generator.LevelType
import level.update.LevelObjectAdd
import level.update.LevelUpdate
import main.Game
import network.ServerNetworkManager
import network.User
import network.packet.*
import player.PlayerManager
import player.lobby.Lobby
import serialization.Id
import serialization.Serialization
import java.util.*

/**
 * As opposed to the [RemoteLevel], an [ActualLevel] is an authoritative instance of the state of the level with the
 * given [id] and [info]. [LevelUpdate]s in this level will be sent over the network to clients in the [currentLobby].
 */
class ActualLevel(id: UUID, info: LevelInfo) : Level(id, info), PacketHandler {

    private constructor() : this(UUID.randomUUID(), LevelInfo(User(UUID.randomUUID(), ""), "", "", LevelType.EMPTY, 0))

    /**
     * The current [Lobby] this level is loaded in. Used for determining who to send [LevelUpdate]s to.
     */
    @Id(13)
    var currentLobby: Lobby? = null

    override fun initialize() {
        if (Game.IS_SERVER) {
            ServerNetworkManager.registerClientPacketHandler(
                this,
                PacketType.REQUEST_LEVEL_DATA,
                PacketType.REQUEST_CHUNK_DATA,
                PacketType.LEVEL_LOADED_SUCCESS
            )
            super.initialize()
        }
    }

    override fun load() {
        val loadedData = LevelManager.tryLoadLevelDataFile(id)
        if (loadedData != null) {
            data = loadedData
            for (chunk in data.chunks) {
                // regenerate tiles because they don't get sent to save space
                chunk.data.tiles = generator.generateTiles(chunk.xChunk, chunk.yChunk)
            }
        } else {
            data = generator.generateData()
        }
        super.load()
    }

    override fun modify(update: LevelUpdate, transient: Boolean): Boolean {
        val success = super.modify(update, transient)
        if (success && !transient) {
            val playersToSendTo = update.playersToSendTo
            currentLobby?.sendPacket(LevelUpdatePacket(update, this), playersToSendTo ?: currentLobby!!.players)
        }
        return true
    }

    override fun handleClientPacket(packet: Packet) {
        if (packet is RequestLevelDataPacket) {
            if (packet.levelId == id) {
                ServerNetworkManager.sendToClient(LevelDataPacket(id, data, updatesCount), packet.connectionId)
            }
        } else if (packet is LevelLoadedSuccessPacket) {
            if (packet.levelId == id) {
                // connect player to lobby
                val player = PlayerManager.getPlayer(packet.fromUser)
                player.lobby.connectPlayer(player)
                if (currentLobby == null) {
                    currentLobby = player.lobby
                } else {
                    currentLobby = player.lobby.merge(currentLobby!!)
                }
            }
        }
    }

    override fun add(l: LevelObject): Boolean {
        val copy = Serialization.copy(l) // TODO add preadding references
        val success = super.modify(LevelObjectAdd(l), false)
        if (success) {
            currentLobby?.sendPacket(LevelUpdatePacket(LevelObjectAdd(copy), this))
        }
        return success
    }

    override fun handleServerPacket(packet: Packet) {
    }
}