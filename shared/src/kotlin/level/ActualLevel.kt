package level

import level.update.LevelObjectAdd
import level.update.LevelUpdate
import main.Game
import network.ServerNetworkManager
import network.packet.*
import player.PlayerManager
import player.lobby.Lobby
import serialization.Serialization
import java.util.*

class ActualLevel(id: UUID, info: LevelInfo) : Level(id, info), PacketHandler {

    override lateinit var data: LevelData

    var currentLobby: Lobby? = null

    init {
        if (Game.IS_SERVER) {
            ServerNetworkManager.registerClientPacketHandler(this, PacketType.REQUEST_LEVEL_DATA, PacketType.REQUEST_CHUNK_DATA, PacketType.LEVEL_LOADED_SUCCESS)
        }
        val loadedData = LevelManager.tryLoadLevelData(id)
        if (loadedData != null) {
            data = loadedData
            for (chunk in data.chunks) {
                // regenerate tiles because they don't get sent to save space
                chunk.data.tiles = generator.generateTiles(chunk.xChunk, chunk.yChunk)
            }
        } else {
            data = generator.generateData(this)
        }
        loaded = true
    }

    override fun modify(update: LevelUpdate, transient: Boolean): Boolean {
        val success = super.modify(update, transient)
        if (success && !transient) {
            val playersToSendTo = update.playersToSendTo
            if (playersToSendTo == null) {
                // level gets to decide
                currentLobby?.sendPacket(LevelUpdatePacket(update, this))
            } else {
                ServerNetworkManager.sendToPlayers(LevelUpdatePacket(update, this), playersToSendTo)
            }
        }
        return true
    }

    override fun add(l: LevelObject): Boolean {
        val copy = Serialization.copy(l) // TODO add preadding references
        val success = super.modify(LevelObjectAdd(l), false)
        if (success) {
            currentLobby?.sendPacket(LevelUpdatePacket(LevelObjectAdd(copy), this))
        }
        return success
    }

    override fun handleClientPacket(packet: Packet) {
        if (packet is RequestLevelDataPacket) {
            if (packet.levelId == id) {
                ServerNetworkManager.sendToClient(LevelDataPacket(id, data, updatesCount), packet.connectionId)
            }
        } else if (packet is LevelLoadedSuccessPacket) {
            println("loaded successfully")
            // connect player to lobby
            val player = PlayerManager.getPlayer(ServerNetworkManager.getUser(packet))
            player.lobby.connectPlayer(player)
            if (currentLobby == null) {
                println("new lobby ${player.lobby}")
                currentLobby = player.lobby
            } else {
                println("merging lobbies ${currentLobby!!.players} with ${player.lobby.players}")
                currentLobby = player.lobby.merge(currentLobby!!)
            }
        }
    }

    override fun handleServerPacket(packet: Packet) {
    }
}