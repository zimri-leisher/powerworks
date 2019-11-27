package level

import item.BlockItemType
import item.ItemType
import main.Game
import network.ServerNetworkManager
import network.packet.*
import player.PlayerManager
import java.util.*

open class ActualLevel(id: UUID, info: LevelInfo) : Level(id, info), PacketHandler {
    override var data = LevelManager.loadLevelData(id) ?: generator.generateData(this)

    init {
        if(Game.IS_SERVER) {
            ServerNetworkManager.registerClientPacketHandler(this, PacketType.REQUEST_CHUNK_DATA, PacketType.REQUEST_LEVEL_DATA, PacketType.PLACE_BLOCK, PacketType.REQUEST_JOIN_LEVEL)
        }
    }

    override fun handleClientPacket(packet: Packet) {
        if (packet is RequestLevelDataPacket) {
            if (packet.id == id) {
                ServerNetworkManager.sendToClient(LevelDataPacket(id, data), packet.connectionId)
            }
        } else if (packet is RequestChunkDataPacket) {
            if (packet.levelId == id) {
                val chunk = data.chunks[packet.xChunk + packet.yChunk * widthChunks]
                ServerNetworkManager.sendToClient(ChunkDataPacket(id, chunk.xChunk, chunk.yChunk, chunk.data), packet.connectionId)
            }
        } else if(packet is PlaceBlockPacket) {
            if(packet.levelId == id) {
                val player = PlayerManager.allPlayers.first { it.user == packet.fromUser }
                println("received a place block packet. block type: ${packet.blockType}")
                val itemType = ItemType.ALL.first { it is BlockItemType && it.placedBlock == packet.blockType }
                if (player.brainRobot.inventory.contains(itemType)) {
                    if (add(packet.blockType.instantiate(packet.xTile shl 4, packet.yTile shl 4, 0))) {
                        player.brainRobot.inventory.remove(itemType)
                        ServerNetworkManager.sendToClients(packet)
                        // broadcast success
                    }
                }
                // return failure
            }
        }
    }

    override fun handleServerPacket(packet: Packet) {
    }
}