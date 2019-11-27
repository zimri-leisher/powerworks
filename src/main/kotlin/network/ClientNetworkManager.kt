package network

import com.esotericsoftware.kryonet.Client
import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.KryoSerialization
import com.esotericsoftware.kryonet.Listener
import data.ConcurrentlyModifiableMutableMap
import main.Game
import main.SERVER_IP
import main.SERVER_PORT
import network.packet.*
import java.lang.ClassCastException
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

object ClientNetworkManager : PacketHandler{
    private lateinit var kryoClient: KryoClient

    private lateinit var connection: Connection

    private lateinit var thread: Thread

    private val running = AtomicBoolean(false)

    private val packetHandlers = ConcurrentlyModifiableMutableMap<PacketHandler, MutableList<PacketType>>()

    private val receivedPackets = Collections.synchronizedList(mutableListOf<Packet>())

    private val outwardPackets = Collections.synchronizedList(mutableListOf<Packet>())

    fun hasConnected() = ::connection.isInitialized

    fun start() {
        registerServerPacketHandler(this, PacketType.SERVER_HANDSHAKE)
        running.set(true)
        thread = thread(isDaemon = true) {
            kryoClient = Client(1048576, 1048576, KryoSerialization(Game.KRYO))
            kryoClient.start()
            kryoClient.setName(Game.USER.id.toString())
            kryoClient.addListener(object : Listener() {
                override fun received(connection: Connection, data: Any?) {
                    if(connection.id == network.ClientNetworkManager.connection.id) {
                        if (data is Packet) {
                            data.connectionId = connection.id
                            receivedPackets.add(data)
                        } else if(data is List<*>) {
                            try {
                                data as Collection<Packet>
                                data.forEach { it.connectionId = connection.id }
                                receivedPackets.addAll(data)
                            } catch (e: ClassCastException) {
                                println("Data is not a packet!")
                            }
                        }
                    }
                }

                override fun connected(connection: Connection) {
                    println("Connected to server at ${connection.remoteAddressTCP}")
                    this@ClientNetworkManager.connection = connection
                    sendToServer(ClientHandshakePacket(System.currentTimeMillis(), Game.USER, Game.VERSION))
                }

                override fun disconnected(connection: Connection) {
                    println("Disconnected from server")
                    running.set(false)
                }
            })
            try {
                kryoClient.connect(5000, SERVER_IP, SERVER_PORT)
            } catch (e: Exception) {
                println("Unable to connect to server at $SERVER_IP:$SERVER_PORT")
            }
        }
    }

    override fun handleClientPacket(packet: Packet) {
    }

    override fun handleServerPacket(packet: Packet) {
        if(packet is ServerHandshakePacket) {
            val receivedTime = System.currentTimeMillis()
            println("Connected to server at $SERVER_IP:$SERVER_PORT in ${packet.serverTimestamp - packet.clientTimestamp} ms")
            println("Latency: ${receivedTime - packet.serverTimestamp} ms")
        }
    }

    fun sendToServer(packet: Packet) {
        outwardPackets.add(packet)
    }

    fun registerServerPacketHandler(handler: PacketHandler, vararg types: PacketType) {
        if (handler !in packetHandlers) {
            packetHandlers[handler] = types.toMutableList()
        } else {
            packetHandlers[handler]!!.addAll(types)
        }
    }

    fun update() {
        synchronized(receivedPackets) {
            for (packet in receivedPackets) {
                packetHandlers.beingTraversed = true
                for ((handler, types) in packetHandlers) {
                    if (packet.type in types) {
                        handler.handleServerPacket(packet)
                    }
                }
                packetHandlers.beingTraversed = false
            }
            receivedPackets.clear()
        }
        if(hasConnected()) {
            synchronized(outwardPackets) {
                for (packet in outwardPackets) {
                    kryoClient.sendTCP(packet)
                }
                outwardPackets.clear()
            }
        }
    }

    fun close() {
        running.set(false)
        thread.join()
        kryoClient.close()
    }
}