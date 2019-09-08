package network

import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.Listener
import main.SERVER_IP
import main.SERVER_PORT
import main.registerKryo
import network.packet.*
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

typealias KryoServer = com.esotericsoftware.kryonet.Server
typealias KryoClient = com.esotericsoftware.kryonet.Client

object Server : PacketHandler{
    private val running = AtomicBoolean(false)

    private lateinit var kryoServer: KryoServer

    private lateinit var thread: Thread

    private val connections = Collections.synchronizedList(mutableListOf<Connection>())

    private val receivedPackets = Collections.synchronizedList(mutableListOf<Packet>())

    private val outwardPackets: MutableMap<Connection?, MutableList<Packet>> = Collections.synchronizedMap(mutableMapOf<Connection?, MutableList<Packet>>())
    private val packetHandlers = mutableMapOf<PacketHandler, MutableList<PacketType>>()

    fun start() {
        registerClientPacketHandler(this, PacketType.CLIENT_HANDSHAKE)
        running.set(true)
        thread = thread(isDaemon = true) {
            kryoServer = KryoServer()
            registerKryo(kryoServer.kryo)
            println("Opening server at $SERVER_IP:$SERVER_PORT")
            kryoServer.bind(SERVER_PORT)
            kryoServer.start()
            kryoServer.addListener(object : Listener() {
                override fun received(connection: Connection, data: Any) {
                    println("receiving data $data")
                    if (data is Packet) {
                        receivedPackets.add(data)
                    }
                }

                override fun connected(connection: Connection) {
                    println("Client at ${connection.remoteAddressTCP} connected")
                    connections.add(connection)
                }
            })
        }
    }

    override fun handleClientPacket(packet: Packet) {
        if(packet is ClientHandshakePacket) {
            sendToClients(ServerHandshakePacket(packet.timestamp, System.currentTimeMillis()))
        }
    }

    override fun handleServerPacket(packet: Packet) {
    }

    fun sendToClient(packet: Packet, connection: Connection) {
        packet.connectionId = connection.id
        outwardPackets.get(connection)?.add(packet) ?: outwardPackets.put(connection, mutableListOf(packet))
    }

    fun sendToClients(packet: Packet) {
        packet.connectionId = -1
        outwardPackets.get(null)?.add(packet) ?: outwardPackets.put(null, mutableListOf(packet))
    }

    fun registerClientPacketHandler(handler: PacketHandler, vararg types: PacketType) {
        if (handler !in packetHandlers) {
            packetHandlers[handler] = types.toMutableList()
        } else {
            packetHandlers[handler]!!.addAll(types)
        }
    }

    fun update() {
        synchronized(receivedPackets) {
            for (packet in receivedPackets) {
                for ((handler, types) in packetHandlers) {
                    if (packet.type in types) {
                        handler.handleClientPacket(packet)
                    }
                }
            }
            receivedPackets.clear()
        }
        synchronized(outwardPackets) {
            for((connection, packet) in outwardPackets) {
                for(connected in connections) {
                    if(connection == null) {
                        kryoServer.sendToAllTCP(packet)
                    } else {
                        kryoServer.sendToTCP(connection.id, packet)
                    }
                }
            }
            outwardPackets.clear()
        }
    }

    fun close() {
        running.set(false)
        kryoServer.stop()
        thread.join()
    }
}