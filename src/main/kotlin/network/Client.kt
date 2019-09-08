package network

import com.esotericsoftware.kryonet.Client
import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.Listener
import main.Game
import main.SERVER_IP
import main.SERVER_PORT
import main.registerKryo
import network.Client.connection
import network.packet.*
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

object Client : PacketHandler{
    private lateinit var kryoClient: KryoClient

    private lateinit var connection: Connection

    private lateinit var thread: Thread

    private val running = AtomicBoolean(false)

    private val packetHandlers = mutableMapOf<PacketHandler, MutableList<PacketType>>()

    private val receivedPackets = Collections.synchronizedList(mutableListOf<Packet>())

    private val outwardPackets = Collections.synchronizedList(mutableListOf<Packet>())
    fun start() {
        running.set(true)
        thread = thread(isDaemon = true) {
            kryoClient = Client()
            registerKryo(kryoClient.kryo)
            kryoClient.start()
            kryoClient.setName(Game.USER.id)
            kryoClient.addListener(object : Listener() {
                override fun received(connection: Connection, data: Any?) {
                    if (data is Packet) {
                        receivedPackets.add(data)
                    }
                }

                override fun connected(connection: Connection) {
                    println("connected to server")
                    this@Client.connection = connection
                    sendToServer(ClientHandshakePacket(System.currentTimeMillis(), Game.USER))
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
        packet.connectionId = connection.id
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
                for ((handler, types) in packetHandlers) {
                    if (packet.type in types) {
                        handler.handleServerPacket(packet)
                    }
                }
            }
            receivedPackets.clear()
        }
        if(this::connection.isInitialized) {
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