package level

import data.ConcurrentlyModifiableMutableList
import item.BlockItemType
import item.ItemType
import level.block.Block
import level.moving.MovingObject
import network.ClientNetworkManager
import network.ServerNetworkManager
import network.packet.*
import player.PlayerManager
import java.util.*

/**
 * A level which is only a copy of another, remote [ActualLevel], usually stored on a [ServerNetworkManager]. This is what the [ClientNetworkManager]
 * keeps as a copy of what the [ServerNetworkManager] has, and is not necessarily correct at any time.
 *
 * @param info the information describing the [Level]
 */
class RemoteLevel(id: UUID, info: LevelInfo) : Level(id, info), PacketHandler {

    override lateinit var data: LevelData

    val packetsWaitingForAck = mutableListOf<Packet>()

    init {
        val chunks: Array<Chunk?> = arrayOfNulls(widthChunks * heightChunks)
        for (y in 0 until heightChunks) {
            for (x in 0 until widthChunks) {
                chunks[x + y * widthChunks] = Chunk(x, y)
            }
        }
        data = LevelData(ConcurrentlyModifiableMutableList(), mutableListOf(), chunks.requireNoNulls(), mutableListOf())
        ClientNetworkManager.registerServerPacketHandler(this, PacketType.REMOVE_MOVING_FROM_LEVEL, PacketType.ADD_DROPPED_ITEM_TO_LEVEL, PacketType.ACK_LEVEL_MODIFICATION, PacketType.CHUNK_DATA, PacketType.LEVEL_DATA, PacketType.UPDATE_BLOCK, PacketType.ADD_MOVING_TO_LEVEL, PacketType.REMOVE_BLOCK, PacketType.ADD_BLOCK_TO_LEVEL)
    }

    override fun add(l: LevelObject): Boolean {
        if (!canAdd(l)) {
            return false
        }
        if (l is Block) {
            l.level = this
            ClientNetworkManager.sendToServer(AddBlockToLevelPacket(l.type, l.xTile, l.yTile, l.rotation, id, PlayerManager.localPlayer).apply { packetsWaitingForAck.add(this) })
        } else if (l is MovingObject) {
            l.level = this
            if (l is DroppedItem) {
                println("adding dropped item")
                ClientNetworkManager.sendToServer(AddDroppedItemToLevel(l.itemType, l.quantity, l.xPixel, l.yPixel, id).apply { packetsWaitingForAck.add(this) })
            } else {
                ClientNetworkManager.sendToServer(AddMovingObjectToLevel(l.type, l.xPixel, l.yPixel, l.rotation, id).apply { packetsWaitingForAck.add(this) })
            }
        }
        return true
    }

    override fun remove(l: LevelObject): Boolean {
        if (!l.inLevel || l.level != this) {
            return false
        }
        if (l is Block) {
            ClientNetworkManager.sendToServer(RemoveBlockFromLevelPacket(l.xTile, l.yTile, l.id, id, PlayerManager.localPlayer).apply { packetsWaitingForAck.add(this) })
        } else if (l is MovingObject) {
            ClientNetworkManager.sendToServer(RemoveMovingFromLevelPacket(l.xPixel, l.yPixel, l.id, id, PlayerManager.localPlayer).apply { packetsWaitingForAck.add(this) })
        }
        return true
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
            if (packet.levelId == id) {
                data = packet.data
                for (chunk in data.chunks) {
                    // regenerate tiles because they don't get sent to save space
                    chunk.data.tiles = generator.generateTiles(chunk.xChunk, chunk.yChunk)
                }
                loaded = true
            }
        } else if (packet is AcknowledgeLevelModificationPacket) {
            if (packet.ackPacketLevelId == id) {
                val ackPacket = packetsWaitingForAck.firstOrNull { it.id == packet.ackPacketId }
                if (ackPacket == null) {
                    println("RECEIVED ACK FOR PACKET THAT WAS NOT WAITING FOR ACK")
                }
                packetsWaitingForAck.remove(ackPacket)
                if (!packet.success) {
                    println("SERVER DENIED $ackPacket")
                    return
                }
            }
        } else if (packet is UpdateBlockPacket) {
            if (packet.block.level == this) {
                forceAdd(packet.block)
            }
        } else if (packet is AddBlockToLevelPacket) {
            if (packet.levelId == id) {
                val block = packet.blockType.instantiate(packet.xTile shl 4, packet.yTile shl 4, packet.rotation)
                super.add(block)
                val itemType = ItemType.ALL.first { it is BlockItemType && it.placedBlock == packet.blockType }
                packet.owner.brainRobot.inventory.remove(itemType)
            }
        } else if (packet is RemoveBlockFromLevelPacket) {
            if (packet.levelId == id) {
                val blockToRemove = getBlockAt(packet.xTile, packet.yTile)
                if (blockToRemove == null) {
                    println("Server said to remove a block at ${packet.xTile}, ${packet.yTile} in $this but there was none there, possible desync")
                } else if (packet.blockId != blockToRemove.id) {
                    println("Server thinks there is a different block at ${packet.xTile}, ${packet.yTile}, possible desync")
                } else if (!super.remove(blockToRemove)) {
                    println("Tried to remove a block that the server said was there but it wasn't")
                    // uh oh desync
                } else {
                    val itemType = ItemType.ALL.first { it is BlockItemType && it.placedBlock == blockToRemove.type }
                    packet.owner.brainRobot.inventory.add(itemType)
                }
            }
        } else if (packet is AddMovingObjectToLevel) {
            if (packet.levelId == id) {
                val moving = packet.movingType.instantiate(packet.xPixel, packet.yPixel, packet.rotation)
                if (!canAdd(moving)) {
                    println("Server said that adding moving $moving was possible but it isn't, possible desync")
                } else {
                    super.add(moving)
                }
            }
        } else if (packet is AddDroppedItemToLevel) {
            if (packet.levelId == id) {
                val droppedItem = DroppedItem(packet.xPixel, packet.yPixel, packet.itemType, packet.quantity)
                if (!canAdd(droppedItem)) {
                    println("Server said that adding dropped item $droppedItem was possible but it isn't, possible desync")
                } else {
                    super.add(droppedItem)
                }
            }
        } else if (packet is RemoveMovingFromLevelPacket) {
            if (packet.levelId == id) {
                val chunk = getChunkFromPixel(packet.xPixel, packet.yPixel)
                val potentialMovings = chunk.data.moving + chunk.data.movingOnBoundary
                if (potentialMovings.isEmpty()) {
                    println("Server said to remove a moving object with id ${packet.movingId} at ${packet.xPixel}, ${packet.yPixel} but there are none there, possible desync")
                } else {
                    val movingToRemove = potentialMovings.firstOrNull { it.id == packet.movingId }
                    if (movingToRemove == null) {
                        println("Server said to remove a moving object with id ${packet.movingId}, but there is no object at its location matching its id")
                        // TODO make this work if the object is moving quickly
                    } else if (!super.remove(movingToRemove)) {
                        println("Unable to remove moving $movingToRemove")
                    } else {
                        println("successfully removed dropped item")
                    }
                }
            }
        }
    }

    override fun handleClientPacket(packet: Packet) {
    }

}
