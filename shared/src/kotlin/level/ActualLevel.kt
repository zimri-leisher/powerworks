package level

import item.BlockItemType
import item.ItemType
import level.moving.MovingObjectType
import main.Game
import network.ServerNetworkManager
import network.packet.*
import java.util.*

open class ActualLevel(id: UUID, info: LevelInfo) : Level(id, info), PacketHandler {
    override var data = LevelManager.tryLoadLevelData(id) ?: generator.generateData(this)

    init {
        if (Game.IS_SERVER) {
            ServerNetworkManager.registerClientPacketHandler(this, PacketType.REMOVE_MOVING_FROM_LEVEL, PacketType.ADD_DROPPED_ITEM_TO_LEVEL, PacketType.ADD_MOVING_TO_LEVEL, PacketType.REQUEST_CHUNK_DATA, PacketType.REQUEST_LEVEL_DATA, PacketType.ADD_BLOCK_TO_LEVEL, PacketType.REMOVE_BLOCK, PacketType.REQUEST_JOIN_LEVEL)
        }
        loaded = true
    }

    override fun handleClientPacket(packet: Packet) {
        if (packet is RequestLevelDataPacket) {
            if (packet.levelId == id) {
                ServerNetworkManager.sendToClient(LevelDataPacket(id, data), packet.connectionId)
            }
        } else if (packet is RequestChunkDataPacket) {
            if (packet.levelId == id) {
                val chunk = data.chunks[packet.xChunk + packet.yChunk * widthChunks]
                ServerNetworkManager.sendToClient(ChunkDataPacket(id, chunk.xChunk, chunk.yChunk, chunk.data), packet.connectionId)
            }
        } else if (packet is AddBlockToLevelPacket) {
            if (packet.levelId == id) {
                var success = false
                // if within reasonable range
                val itemType = ItemType.ALL.first { it is BlockItemType && it.placedBlock == packet.blockType }
                if (packet.owner.brainRobot.inventory.contains(itemType)) {
                    if (canAdd(packet.blockType, packet.xTile shl 4, packet.yTile shl 4)) {
                        val block = packet.blockType.instantiate(packet.xTile shl 4, packet.yTile shl 4, packet.rotation)
                        this.add(block)
                        packet.owner.brainRobot.inventory.remove(itemType)
                        success = true
                    }
                }
                ServerNetworkManager.sendToClient(AcknowledgeLevelModificationPacket(id, packet.id, success), packet.connectionId)
                ServerNetworkManager.sendToClients(packet)
            }
        } else if (packet is RemoveBlockFromLevelPacket) {
            if (packet.levelId == id) {
                var success = false
                val blockToRemove = getBlockAt(packet.xTile, packet.yTile)
                if(blockToRemove == null) {
                    println("Client said to remove a block at ${packet.xTile}, ${packet.yTile} in $this but there was none there")
                } else {
                    // if within reasonable range
                    val itemType = ItemType.ALL.first { it is BlockItemType && it.placedBlock == blockToRemove.type }
                    if (packet.owner.brainRobot.inventory.spaceFor(itemType)) {
                        if (remove(blockToRemove)) {
                            packet.owner.brainRobot.inventory.add(itemType)
                            ServerNetworkManager.sendToClients(packet)
                            success = true
                        }
                    }
                }
                ServerNetworkManager.sendToClient(AcknowledgeLevelModificationPacket(id, packet.id, success), packet.connectionId)
            }
        } else if(packet is AddMovingObjectToLevel) {
            if(packet.levelId == id) {
                var success = false
                if(canAdd(packet.movingType, packet.xPixel, packet.yPixel)) {
                    val moving = packet.movingType.instantiate(packet.xPixel, packet.yPixel, packet.rotation)
                    add(moving)
                    success = true
                    ServerNetworkManager.sendToClients(packet)
                }
                ServerNetworkManager.sendToClient(AcknowledgeLevelModificationPacket(id, packet.id, success), packet.connectionId)
            }
        } else if(packet is AddDroppedItemToLevel) {
            if(packet.levelId == id) {
                var success = false
                if(canAdd(MovingObjectType.DROPPED_ITEM, packet.xPixel, packet.yPixel)) {
                    val droppedItem = DroppedItem(packet.xPixel, packet.yPixel, packet.itemType, packet.quantity)
                    println("added dropped item at ${packet.xPixel} ${packet.yPixel}")
                    add(droppedItem)
                    success = true
                    ServerNetworkManager.sendToClients(packet)
                }
                ServerNetworkManager.sendToClient(AcknowledgeLevelModificationPacket(id, packet.id, success), packet.connectionId)
            }
        } else if(packet is RemoveMovingFromLevelPacket) {
            if(packet.levelId == id) {
                var success = false
                val chunk = getChunkFromPixel(packet.xPixel, packet.yPixel)
                val potentialMovings = chunk.data.moving + chunk.data.movingOnBoundary
                println(potentialMovings.joinToString())
                if(potentialMovings.isNotEmpty()) {
                    val movingToRemove = potentialMovings.firstOrNull { it.id == packet.movingId }
                    println("moving object to remove: $movingToRemove")
                    if(movingToRemove != null) {
                        if(super.remove(movingToRemove)) {
                            success = true
                            println("success")
                            ServerNetworkManager.sendToClients(packet)
                        }
                    }
                }
                ServerNetworkManager.sendToClient(AcknowledgeLevelModificationPacket(id, packet.id, success), packet.connectionId)
            }
        }
    }

    override fun handleServerPacket(packet: Packet) {
    }
}