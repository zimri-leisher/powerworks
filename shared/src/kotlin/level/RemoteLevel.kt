package level

import level.generator.LevelType
import level.update.LevelObjectAdd
import level.update.LevelObjectRemove
import level.update.LevelUpdate
import main.removeIfKey
import network.ClientNetworkManager
import network.ServerNetworkManager
import network.User
import network.packet.*
import serialization.Serialization
import java.util.*

data class IncomingLevelUpdate(val update: LevelUpdate, var actedOn: Boolean, val receivedOn: Int)

/**
 * A level which is only a copy of another, remote [ActualLevel], usually stored on a [ServerNetworkManager]. This is what the [ClientNetworkManager]
 * keeps as a copy of what the [ServerNetworkManager] has, and is not necessarily correct at any time. Instantiating this
 * will not automatically load it, [requestLoad] must be called first
 *
 * @param info the information describing the [Level]
 */
class RemoteLevel(id: UUID, info: LevelInfo) : Level(id, info), PacketHandler {

    private constructor() : this(UUID.randomUUID(), LevelInfo(User(UUID.randomUUID(), ""), "", "", LevelType.EMPTY, 0))

    val outgoingUpdates = mutableMapOf<LevelUpdate, Int>()

    // level update - the update, boolean - whether or not it was acted on, int - time received
    val incomingUpdates = mutableListOf<IncomingLevelUpdate>()

    override fun initialize() {
        println("initialize remote level with id $id")
        ClientNetworkManager.registerServerPacketHandler(
            this,
            PacketType.LEVEL_UPDATE,
            PacketType.CHUNK_DATA,
            PacketType.LEVEL_DATA,
            PacketType.UPDATE_BLOCK
        )
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

    override fun modify(update: LevelUpdate, transient: Boolean): Boolean {
        if (!transient && incomingUpdates.any { incomingUpdate -> incomingUpdate.update.equivalent(update) && incomingUpdate.actedOn }) {
            // if there are any incoming updates that are equivalent to this action and have already been taken
            // return true because this action has already happened
            return true
        }
        if (!canModify(update)) {
            return false
        }
        if (transient) {
            super.modify(update, transient)
        } else {
            val equivalent = incomingUpdates.filter { it.update.equivalent(update) }
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
                // if this update has been outgoing for 5 seconds and still hasn't been acted on, just cancel it
                update.cancelActGhost()
                outgoingIterator.remove()
            }
        }
        val incomingIterator = incomingUpdates.iterator()
        for (incomingUpdate in incomingIterator) {
            if(!Serialization.isResolved(incomingUpdate.update)) {
                Serialization.resolveReferences(incomingUpdate.update)
                continue
            }
            // find all updates that the client had that are equivalent to this incoming update
            val equivalentOutgoings = outgoingUpdates.filterKeys { it.equivalent(incomingUpdate.update) }
            // cancel their ghost actions
            equivalentOutgoings.forEach { t, u -> t.cancelActGhost() }
            // remove them from list of outgoing
            outgoingUpdates.removeIfKey { it in equivalentOutgoings }
            // try to take this action
            if (!incomingUpdate.actedOn && super.modify(incomingUpdate.update, false)) {
                incomingIterator.remove()
                incomingUpdate.actedOn = true
            } else {
                if (!incomingUpdate.actedOn && (updatesCount - incomingUpdate.receivedOn) % 10 == 0) {
//                    update.resolveReferences()
                    // todo figure out way to repeatedly resolve references... or just crash if they don't resolve
                }
                if (updatesCount - incomingUpdate.receivedOn > 60) {
                    incomingIterator.remove()
                    if (!incomingUpdate.actedOn) {
                        println("client has been unable to take server action $incomingUpdate for more than a second")
                    }
                }
            }
        }
    }

    override fun handleServerPacket(packet: Packet) {
        if (packet.type == PacketType.LEVEL_UPDATE) {
            packet as LevelUpdatePacket
            if (packet.update.level == this) {
                incomingUpdates.add(IncomingLevelUpdate(packet.update, false, updatesCount))
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
                println("received level data with brain robots ${packet.data.brainRobots}")
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
