package network

import com.esotericsoftware.kryonet.Client
import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.Listener
import data.ConcurrentlyModifiableMutableMap
import main.*
import network.packet.*
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.math.pow

object ClientNetworkManager : PacketHandler {

    private lateinit var kryoClient: KryoClient

    private val connectionDelegate = PowerworksDelegates.lateinitVal<Connection>()

    private var connection: Connection by connectionDelegate

    private lateinit var thread: Thread

    private val running = AtomicBoolean(false)

    val handlingLock = Any()

    private val packetHandlers = ConcurrentlyModifiableMutableMap<PacketHandler, MutableList<PacketType>>()

    private val receivedPackets = Collections.synchronizedList(mutableListOf<Packet>())

    private val outwardPackets = Collections.synchronizedList(mutableListOf<Packet>())

    val visibleOnlineUsers = mutableListOf<User>()

    fun hasConnected() = connectionDelegate.initialized

    fun start() {
        registerServerPacketHandler(this, PacketType.SERVER_HANDSHAKE)
        running.set(true)
        thread = thread(isDaemon = true) {
            kryoClient = Client(2.0.pow(21.0).toInt(), 2.0.pow(21.0).toInt(), NetworkSerializationPlug())
            kryoClient.start()
            visibleOnlineUsers.add(Game.USER)
            kryoClient.setName(Game.USER.id.toString())
            kryoClient.addListener(object : Listener() {
                override fun received(connection: Connection, data: Any?) {
                    if (connection.id == ClientNetworkManager.connection.id) {
                        if (data is Packet) {
                            data.connectionId = connection.id
                            receivedPackets.add(data)
                            println("received packet $data")
                        } else if (data is List<*>) {
                            try {
                                data as Collection<Packet>
                                data.forEach { it.connectionId = connection.id }
                                receivedPackets.addAll(data)
                                println("received packets $data")
                            } catch (e: ClassCastException) {
                                println("Data is not a packet!")
                            }
                        }
                    }
                }

                override fun connected(connection: Connection) {
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

    fun isVisibleAndOnline(user: User) = user in visibleOnlineUsers

    override fun handleClientPacket(packet: Packet) {
    }

    override fun handleServerPacket(packet: Packet) {
        if (packet is ServerHandshakePacket) {
            val receivedTime = System.currentTimeMillis()
            println("Connected to server at $SERVER_IP:$SERVER_PORT in ${packet.serverTimestamp - packet.clientTimestamp} ms")
            println("Latency: ${receivedTime - packet.serverTimestamp} ms TEST ${connection.returnTripTime}")
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
        synchronized(handlingLock) {
            synchronized(receivedPackets) {
                for (packet in receivedPackets) {
                    packetHandlers.forEach {handler, types ->
                        if(packet.type in types) {
                            handler.handleServerPacket(packet)
                        }
                    }
                }
                receivedPackets.clear()
            }
        }
        if (hasConnected()) {
            synchronized(outwardPackets) {
                for (packet in outwardPackets) {
                    println("sending packet $packet")
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