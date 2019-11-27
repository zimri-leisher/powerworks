package network

import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.KryoSerialization
import com.esotericsoftware.kryonet.Listener
import data.ConcurrentlyModifiableMutableMap
import main.*
import network.packet.*
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

    private val outwardPackets: MutableMap<Int?, MutableList<Packet>> = Collections.synchronizedMap(mutableMapOf<Int?, MutableList<Packet>>())
    private val packetHandlers = ConcurrentlyModifiableMutableMap<PacketHandler, MutableList<PacketType>>()

    fun start() {
        registerClientPacketHandler(this, PacketType.CLIENT_HANDSHAKE)
        running.set(true)
        thread = thread(isDaemon = true) {
            kryoServer = KryoServer(1048576, 1048576, KryoSerialization(Game.KRYO))
            println("Opening server at $SERVER_IP:$SERVER_PORT")
            kryoServer.bind(SERVER_PORT)
            kryoServer.start()
            kryoServer.addListener(object : Listener() {
                override fun received(connection: Connection, data: Any) {
                    println("received $data")
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
        packet.onSend = onSend
        outwardPackets.get(connectionId)?.add(packet) ?: outwardPackets.put(connectionId, mutableListOf(packet))
    }

    fun sendToClients(packet: Packet, onSend: Packet.() -> Unit = {}) {
        packet.connectionId = -1
        packet.onSend = onSend
        outwardPackets.get(null)?.add(packet) ?: outwardPackets.put(null, mutableListOf(packet))
    }

    fun registerClientPacketHandler(handler: PacketHandler, vararg types: PacketType) {
        if (handler !in packetHandlers) {
            packetHandlers[handler] = types.toMutableList()
        } else {
            packetHandlers[handler]!!.addAll(types)
        }
    }

    fun getConnection(id: Int) = connections.first { println("${it.id}, $id"); it.id == id }

    fun getUser(packet: Packet) = getUser(packet.connectionId)

    fun getUser(connection: Int) = clientInfos[connection]!!.user

    fun isAccepted(connection: Int) = connection in clientInfos

    fun update() {
        synchronized(receivedPackets) {
            val iterator = receivedPackets.iterator()
            for (packet in iterator) {
                if (isAccepted(packet.connectionId) || packet is ClientHandshakePacket) {
                    packetHandlers.beingTraversed = true
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
        synchronized(outwardPackets) {
            val iterator = outwardPackets.iterator()
            for ((connectionId, packet) in iterator) {
                if (connectionId == null) {
                    for (accepted in clientInfos.map { it.key }) {
                        kryoServer.sendToTCP(accepted, packet)
                    }
                    packet.forEach { it.onSend(it) }
                    iterator.remove()
                } else {
                    if(isAccepted(connectionId)) {
                        kryoServer.sendToTCP(connectionId, packet)
                        packet.forEach { it.onSend(it) }
                        iterator.remove()
                    }
                }
            }
        }
    }

    fun close() {
        running.set(false)
        kryoServer.stop()
        thread.join()
    }
}