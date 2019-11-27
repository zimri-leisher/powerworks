package level

import network.ClientNetworkManager
import network.ServerNetworkManager
import network.packet.*
import data.ConcurrentlyModifiableMutableList
import java.util.*

/**
 * A level which is only a copy of another, remote [MasterLevel], usually stored on a [ServerNetworkManager]. This is what the [ClientNetworkManager]
 * keeps as a copy of what the [ServerNetworkManager] has, and is not necessarily correct at any time.
 *
 * @param info the information describing the [Level]
 */
class RemoteLevel(id: UUID, info: LevelInfo) : Level(id, info), PacketHandler {

    override lateinit var data: LevelData

    /**
     * If true, this level has received a complete [LevelDataPacket] at least once from the [ServerNetworkManager]
     */
    var loaded = false
        private set

    init {
        val chunks: Array<Chunk?> = arrayOfNulls(widthChunks * heightChunks)
        for (y in 0 until heightChunks) {
            for (x in 0 until widthChunks) {
                chunks[x + y * widthChunks] = Chunk(x, y)
            }
        }
        data = LevelData(ConcurrentlyModifiableMutableList(), mutableListOf(), chunks.requireNoNulls())
        ClientNetworkManager.registerServerPacketHandler(this, PacketType.CHUNK_DATA, PacketType.LEVEL_DATA, PacketType.PLACE_BLOCK)
    }

    override fun handleServerPacket(packet: Packet) {
        if (packet is ChunkDataPacket) {
            if (packet.levelId == id) {
                data.chunks[packet.xChunk + packet.yChunk * widthChunks] = Chunk(packet.xChunk, packet.yChunk).apply {
                    // tile data is not sent, so we want to keep the old tile data. The tiles array here is never modified,
                    // only generated from the seed when this level is initially loaded. The way you change tiles is through the
                    // modifiedTiles list, which is almost always significantly less memory, and thus easier to send, than the
                    // tiles array
                    packet.chunkData.tiles = data.tiles
                    data = packet.chunkData
                }
            }
        } else if (packet is LevelDataPacket) {
            if (packet.id == id) {
                data = packet.data
                for(chunk in data.chunks) {
                    // regenerate tiles because they don't get sent to save space
                    chunk.data.tiles = generator.generateTiles(chunk.xChunk, chunk.yChunk)
                }
                loaded = true
            }
        } else if(packet is PlaceBlockPacket) {
            if(!add(packet.blockType.instantiate(packet.xTile shl 4, packet.yTile shl 4, 0))) {
                // uh oh, desync somewhere. rehash and update chunks
            }
        }
    }

    override fun handleClientPacket(packet: Packet) {
    }

}
