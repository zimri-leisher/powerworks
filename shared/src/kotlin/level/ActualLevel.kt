package level

import main.Game
import network.ServerNetworkManager
import network.packet.*
import serialization.Serialization
import java.util.*

class ActualLevel(id: UUID, info: LevelInfo) : Level(id, info), PacketHandler {

    override lateinit var data: LevelData
    var isLoadedOnClient = false

    init {
        if (Game.IS_SERVER) {
            ServerNetworkManager.registerClientPacketHandler(this, PacketType.REQUEST_LEVEL_DATA, PacketType.REQUEST_CHUNK_DATA)
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

    override fun modify(modification: LevelModification, transient: Boolean): Boolean {
        val success = super.modify(modification, transient)
        if (success && !transient && isLoadedOnClient) {
            ServerNetworkManager.sendToClients(LevelModificationPacket(modification, this))
        }
        return true
    }

    override fun add(l: LevelObject): Boolean {
        val copy = Serialization.copy(l) // TODO add preadding references
        val success = super.modify(AddObject(l), false)
        if (success && isLoadedOnClient) {
            ServerNetworkManager.sendToClients(LevelModificationPacket(AddObject(copy), this))
        }
        return success
    }

    override fun handleClientPacket(packet: Packet) {
        if (packet is RequestLevelDataPacket) {
            if (packet.levelId == id) {
                ServerNetworkManager.sendToClient(LevelDataPacket(id, data, updatesCount), packet.connectionId)
                isLoadedOnClient = true
            }
        } else if (packet is RequestChunkDataPacket) {
            if (packet.levelId == id) {
                val chunk = data.chunks[packet.xChunk + packet.yChunk * widthChunks]
                ServerNetworkManager.sendToClient(ChunkDataPacket(id, chunk.xChunk, chunk.yChunk, chunk.data), packet.connectionId)
            }
        }
    }

    override fun handleServerPacket(packet: Packet) {
    }
}