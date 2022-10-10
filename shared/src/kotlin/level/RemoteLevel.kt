package level

import level.update.LevelObjectAdd
import level.update.LevelObjectRemove
import level.update.LevelUpdate
import main.removeIfKey
import network.ClientNetworkManager
import network.ServerNetworkManager
import network.packet.*
import java.util.*

/**
 * A level which is only a copy of another, remote [ActualLevel], usually stored on a [ServerNetworkManager]. This is what the [ClientNetworkManager]
 * keeps as a copy of what the [ServerNetworkManager] has, and is not necessarily correct at any time. Instantiating this
 * will not automatically load it, [requestLoad] must be called first
 *
 * @param info the information describing the [Level]
 */
class RemoteLevel(id: UUID, info: LevelInfo) : Level(id, info), PacketHandler {

    val outgoingUpdates = mutableMapOf<LevelUpdate, Int>()

    // level update - the update, boolean - whether or not it was acted on, int - time received
    val incomingUpdates = mutableMapOf<Pair<LevelUpdate, Boolean>, Int>()

    override fun initialize() {
        println("initialize remote level with id $id")
        ClientNetworkManager.registerServerPacketHandler(this, PacketType.LEVEL_UPDATE, PacketType.CHUNK_DATA, PacketType.LEVEL_DATA, PacketType.UPDATE_BLOCK)
        super.initialize()
    }

    override fun load() {
        val chunks: Array<Chunk?> = arrayOfNulls(widthChunks * heightChunks)
        for (y in 0 until heightChunks) {
            for (x in 0 until widthChunks) {
                chunks[x + y * widthChunks] = Chunk(x, y)
            }
        }
        ClientNetworkManager.sendToServer(RequestLevelDataPacket(id))
    }

    override fun add(l: LevelObject): Boolean {
        return modify(LevelObjectAdd(l), l is GhostLevelObject) // transient if l is ghost object
    }

    override fun remove(l: LevelObject): Boolean {
        return modify(LevelObjectRemove(l), l is GhostLevelObject) // transient if l is ghost object
    }

    override fun modify(update: LevelUpdate, transient: Boolean): Boolean {
        if (!transient && incomingUpdates.any { (updateAndHasActed, _) -> updateAndHasActed.first.equivalent(update) && updateAndHasActed.second }) {
            // if there are any incoming updates that are equivalent to this action and have already been taken
            // return true because this action has already happened
            return true
        }
        if (!canModify(update)) {
            return false
        }
        if (transient) {
            update.act()
        } else {
            val equivalent = incomingUpdates.keys.filter { it.first.equivalent(update) }
            if (equivalent.isEmpty()) {
                update.actGhost()
                outgoingUpdates.put(update, updatesCount)
            } else {
                // there are equivalent modifications waiting to be taken that are from the server
                // take them instead, ignore this
            }
        }
        return true
    }

    override fun update() {
        super.update()
        val outgoingIterator = outgoingUpdates.iterator()
        for ((update, time) in outgoingIterator) {
            if (updatesCount - time > 300) {
                update.cancelActGhost()
                outgoingIterator.remove()
            }
        }
        val incomingIterator = incomingUpdates.iterator()
        val updatesActedOn = mutableMapOf<Pair<LevelUpdate, Boolean>, Int>()
        for ((updateAndHasActed, time) in incomingIterator) {
            val (update, hasActed) = updateAndHasActed
            val equivalentOutgoings = outgoingUpdates.filterKeys { it.equivalent(update) }
            equivalentOutgoings.forEach { t, u -> t.cancelActGhost() }
            outgoingUpdates.removeIfKey { it in equivalentOutgoings }
            if (!hasActed && update.canAct()) {
                update.act()
                incomingIterator.remove()
                updatesActedOn.put(update to true, time)
            } else {
                if (!hasActed && (updatesCount - time) % 10 == 0) {
                    update.resolveReferences()
                }
                if (updatesCount - time > 60) {
                    incomingIterator.remove()
                    if (!hasActed) {
                        println("client has been unable to take server action $update for more than a second")
                    }
                }
            }
        }
        incomingUpdates.removeIfKey { it.first in updatesActedOn.keys.map { inner -> inner.first } }
        updatesActedOn.forEach { incomingUpdates.put(it.key, it.value) }
    }

    override fun handleServerPacket(packet: Packet) {
        if (packet.type == PacketType.LEVEL_UPDATE) {
            packet as LevelUpdatePacket
            if (packet.level == this) {
                incomingUpdates.put(packet.update to false, updatesCount)
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
