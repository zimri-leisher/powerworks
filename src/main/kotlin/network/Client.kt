package network

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Net
import com.badlogic.gdx.net.Socket
import java.io.*
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

object Client : PacketHandler {
    val running = AtomicBoolean(false)

    lateinit var socket: Socket

    lateinit var output: DataOutputStream
    lateinit var input: DataInputStream

    lateinit var networkAddresses: List<String>

    lateinit var thread: Thread

    val receivedPackets = Collections.synchronizedList(mutableListOf<Packet>())
    private val packetHandlers = mutableMapOf<PacketHandler, List<PacketType>>()

    val outwardPackets = Collections.synchronizedList(mutableListOf<Packet>())

    fun start() {
        NetworkManager.client = true
        running.set(true)
        registerPacketHandler(this, PacketType.SERVER_HANDSHAKE)
        findNetworkInterfaces()
        thread = thread(start = true, isDaemon = true) {
            socket = Gdx.net.newClientSocket(Net.Protocol.TCP, "127.0.0.1", 9412, null)
            println("Connected to server")
            // sends output to the socket
            output = DataOutputStream(socket.outputStream)
            input = DataInputStream(socket.inputStream)
            val currentTime = System.currentTimeMillis()
            sendToServer(ClientHandshakePacket(currentTime))
            while (running.get()) {

                val nextPacketType = input.readInt()
                if (nextPacketType == -1) {
                    close()
                    break
                }
                try {
                    val newPacket = PacketType.read(nextPacketType, input)
                    receivedPackets.add(newPacket)
                } catch (e: InvalidObjectException) {
                    println("Invalid packet received")
                }
            }
            output.close()
            input.close()
        }
    }

    @Synchronized
    fun close() {
        running.set(false)
        thread.join()
    }

    override fun handlePacket(packet: Packet) {
        if(packet is ServerHandshakePacket) {
            println("Latency: ${System.currentTimeMillis() - packet.serverTimestamp} ms")
            println("Time to connect: ${packet.serverTimestamp - packet.clientTimestamp} ms")
        }
    }

    fun sendToServer(packet: Packet) {
        outwardPackets.add(packet)
    }

    fun registerPacketHandler(handler: PacketHandler, vararg types: PacketType) {
        packetHandlers.put(handler, listOf(*types))
    }

    fun removePacketHandler(handler: PacketHandler) {
        packetHandlers.remove(handler)
    }

    private fun findNetworkInterfaces() {
        val addresses = mutableListOf<String>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (ni in Collections.list(interfaces)) {
                for (address in Collections.list(ni.inetAddresses)) {
                    if (address is Inet4Address) {
                        addresses.add(address.getHostAddress())
                    }
                }
            }
        } catch (e: SocketException) {
            e.printStackTrace()
        }
        networkAddresses = addresses
    }

    fun hasConnected() = socket.isConnected

    fun update() {
        if (!hasConnected())
            return
        synchronized(receivedPackets) {
            for (packet in receivedPackets) {
                for ((handler, types) in packetHandlers) {
                    if (packet.type in types) {
                        handler.handlePacket(packet)
                    }
                }
            }
            receivedPackets.clear()
        }
        synchronized(outwardPackets) {
            for (packet in outwardPackets) {
                packet.write(output)
                output.flush()
            }
            outwardPackets.clear()
        }
    }
}