package level

import data.ConcurrentlyModifiableMutableList
import main.removeIfKey
import network.ClientNetworkManager
import network.ServerNetworkManager
import network.packet.*
import java.util.*

/**
 * A level which is only a copy of another, remote [ActualLevel], usually stored on a [ServerNetworkManager]. This is what the [ClientNetworkManager]
 * keeps as a copy of what the [ServerNetworkManager] has, and is not necessarily correct at any time.
 *
 * @param info the information describing the [Level]
 */
class RemoteLevel(id: UUID, info: LevelInfo) : Level(id, info), PacketHandler {

    override lateinit var data: LevelData

    val outgoingModifications = mutableMapOf<LevelModification, Int>()
    val incomingModifications = mutableMapOf<LevelModification, Int>()

    init {
        val chunks: Array<Chunk?> = arrayOfNulls(widthChunks * heightChunks)
        for (y in 0 until heightChunks) {
            for (x in 0 until widthChunks) {
                chunks[x + y * widthChunks] = Chunk(x, y)
            }
        }
        data = LevelData(ConcurrentlyModifiableMutableList(), ConcurrentlyModifiableMutableList(), chunks.requireNoNulls(), mutableListOf())
        ClientNetworkManager.registerServerPacketHandler(this, PacketType.LEVEL_UPDATE, PacketType.CHUNK_DATA, PacketType.LEVEL_DATA, PacketType.UPDATE_BLOCK)
    }

    override fun add(l: LevelObject): Boolean {
        return modify(AddObject(l), l is GhostLevelObject) // transient if l is ghost object
    }

    override fun remove(l: LevelObject): Boolean {
        return modify(RemoveObject(l), l is GhostLevelObject) // transient if l is ghost object
    }

    override fun modify(modification: LevelModification, transient: Boolean): Boolean {
        if (!canModify(modification)) {
            return false
        }
        if (transient) {
            modification.act(this)
        } else {
            val equivalent = incomingModifications.keys.filter { it.equivalent(modification) }
            if (equivalent.isEmpty()) {
                modification.actGhost(this)
                outgoingModifications.put(modification, updatesCount)
            } else {
                // this has already happened
                incomingModifications.removeIfKey { it in equivalent }
            }
        }
        return true
    }

    override fun update() {
        if (paused) {
            return
        }
        super.update()
        val outgoingIterator = outgoingModifications.iterator()
        for ((_, time) in outgoingIterator) {
            if (updatesCount - time > 300) {
                outgoingIterator.remove()
            }
        }
        val incomingIterator = incomingModifications.iterator()
        for ((_, time) in incomingIterator) {
            if (updatesCount - time > 300) {
                incomingIterator.remove()
            }
        }
    }

    override fun handleServerPacket(packet: Packet) {
        if (packet.type == PacketType.LEVEL_UPDATE) {
            packet as LevelUpdatePacket
            if (packet.level == this) {
                if (packet is LevelModificationPacket) {
                    val equivalentModifications = outgoingModifications.keys.filter {
                        it.equivalent(packet.modification)
                    }
                    if (equivalentModifications.isEmpty()) {
                        packet.modification.act(this)
                        incomingModifications.put(packet.modification, updatesCount)
                    } else {
                        val localMod = equivalentModifications.first()
                        localMod.act(this)
                    }
                    outgoingModifications.removeIfKey { it in equivalentModifications }
                }
            }
        } else if (packet is ChunkDataPacket) {
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
            if (packet.levelId == id) {
                data = packet.data
                for (chunk in data.chunks) {
                    // regenerate tiles because they don't get sent to save space
                    chunk.data.tiles = generator.generateTiles(chunk.xChunk, chunk.yChunk)
                }
                updatesCount = packet.updatesCount
                loaded = true
                println("received level data, communicating success")
                ClientNetworkManager.sendToServer(LevelLoadedSuccessPacket(id))
            }
        } else if (packet is UpdateBlockPacket) {
            if (packet.block.level == this) {
                // todo remove?
            }
        }
    }

    override fun handleClientPacket(packet: Packet) {
    }

}
