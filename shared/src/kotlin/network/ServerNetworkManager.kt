package network

import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.KryoSerialization
import com.esotericsoftware.kryonet.Listener
import com.esotericsoftware.kryonet.Serialization
import data.ConcurrentlyModifiableMutableMap
import main.*
import network.packet.*
import serialization.SerializerDebugger
import java.lang.Exception
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

typealias KryoServer = com.esotericsoftware.kryonet.Server
typealias KryoClient = com.esotericsoftware.kryonet.Client

data class ClientInfo(val user: User, val version: Version, val address: String)

object ServerNetworkManager : PacketHandler {
    private val running = AtomicBoolean(false)

    private lateinit var kryoServer: KryoServer

    private lateinit var thread: Thread

    private val connections = Collections.synchronizedList(mutableListOf<Connection>())

    private val clientInfos = Collections.synchronizedMap(mutableMapOf<Int, ClientInfo>())

    private val receivedPackets = Collections.synchronizedList(mutableListOf<Packet>())

    private var traversingOutwardPackets = false
    private val _toAddOutwardPackets: MutableMap<Int?, MutableList<Packet>> = Collections.synchronizedMap(mutableMapOf<Int?, MutableList<Packet>>())
    private val outwardPackets: MutableMap<Int?, MutableList<Packet>> = Collections.synchronizedMap(mutableMapOf<Int?, MutableList<Packet>>())
    private val packetHandlers = ConcurrentlyModifiableMutableMap<PacketHandler, MutableList<PacketType>>()

    fun start() {
        registerClientPacketHandler(this, PacketType.CLIENT_HANDSHAKE)
        running.set(true)
        thread = thread(isDaemon = true) {
            try {
                kryoServer = KryoServer(1048576, 1048576, NetworkSerializationPlug())
                println("Opening server at $SERVER_IP:$SERVER_PORT")
                kryoServer.bind(SERVER_PORT)
                kryoServer.start()
                kryoServer.addListener(object : Listener() {
                    override fun received(connection: Connection, data: Any) {
                        if (data is Packet) {
                            data.connectionId = connection.id
                            receivedPackets.add(data)
                        }
                    }

                    override fun connected(connection: Connection) {
                        println("Client at ${connection.remoteAddressTCP} connected")
                        connections.add(connection)
                    }

                    override fun disconnected(connection: Connection) {
                        println("Client id ${connection.id} disconnected")
                    }
                })
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    override fun handleClientPacket(packet: Packet) {
        if (packet is ClientHandshakePacket) {
            if (clientInfos.none { (id, _) -> id == packet.connectionId }) {
                val accepted = Game.VERSION.isCompatible(packet.version)
                sendToClient(ServerHandshakePacket(packet.timestamp, System.currentTimeMillis(), accepted), packet.connectionId) {
                    if (!accepted) connection.close()
                }
                if (accepted) {
                    clientInfos.put(packet.connectionId, ClientInfo(packet.newUser, packet.version, packet.connection.remoteAddressTCP.hostString))
                }
            }
        }
    }

    override fun handleServerPacket(packet: Packet) {
    }

    fun sendToClient(packet: Packet, connectionId: Int, onSend: Packet.() -> Unit = {}) {
        if(!Game.IS_SERVER) return
        packet.onSend = onSend
        // want to put in the correct map a pair of (connectionId, packet)
        (if(traversingOutwardPackets) _toAddOutwardPackets else outwardPackets).get(connectionId)?.add(packet) ?: outwardPackets.put(connectionId, mutableListOf(packet))
    }

    fun sendToClients(packet: Packet, onSend: Packet.() -> Unit = {}) {
        if(!Game.IS_SERVER) return
        packet.connectionId = -1
        packet.onSend = onSend
        // want to put in the correct map a pair of (null, packet)
        // null value signifies it goes to all clients
        (if(traversingOutwardPackets) _toAddOutwardPackets else outwardPackets).get(null)?.add(packet) ?: outwardPackets.put(null, mutableListOf(packet))
    }

    fun registerClientPacketHandler(handler: PacketHandler, vararg types: PacketType) {
        if(!Game.IS_SERVER) return
        if (handler !in packetHandlers) {
            packetHandlers[handler] = types.toMutableList()
        } else {
            packetHandlers[handler]!!.addAll(types)
        }
    }

    fun getConnection(id: Int) = connections.first { it.id == id }

    fun getUser(packet: Packet) = getUser(packet.connectionId)

    fun getUser(connection: Int) = clientInfos[connection]!!.user

    fun isAccepted(connection: Int) = connection in clientInfos

    fun update() {
        // handle packets received
        synchronized(receivedPackets) {
            val iterator = receivedPackets.iterator()
            for (packet in iterator) {
                // make sure the client has already been accepted or it is in the process of accepting (and so is sending a ClientHandshakePacket)
                if (isAccepted(packet.connectionId) || packet is ClientHandshakePacket) {
                    packetHandlers.beingTraversed = true
                    // send packet to appropriate handlers
                    for ((handler, types) in packetHandlers) {
                        if (packet.type in types) {
                            handler.handleClientPacket(packet)
                        }
                    }
                    packetHandlers.beingTraversed = false
                    iterator.remove()
                }
            }
        }
        // send out all packets to appropriate clients
        synchronized(outwardPackets) {
            traversingOutwardPackets = true
            val iterator = outwardPackets.iterator()
            for ((connectionId, packets) in iterator) {
                // if the packet is meant for all clients
                if (connectionId == null) {
                    // send it to all accepted clients
                    for (accepted in clientInfos.map { it.key }) {
                        packets.forEach { kryoServer.sendToTCP(accepted, it) }
                    }
                    packets.forEach { it.onSend(it) }
                    iterator.remove()
                } else {
                    // else if the packet is meant for one client (connectionId)
                    // verify the connectionId is accepted
                    if(isAccepted(connectionId)) {
                        // send to client
                        packets.forEach { kryoServer.sendToTCP(connectionId, it) }
                        packets.forEach { it.onSend(it) }
                        iterator.remove()
                    }
                }
            }
            traversingOutwardPackets = false
            _toAddOutwardPackets.forEach { outwardPackets.put(it.key, it.value) }
            _toAddOutwardPackets.clear()
        }
    }

    fun close() {
        running.set(false)
        kryoServer.stop()
        thread.join()
    }
}