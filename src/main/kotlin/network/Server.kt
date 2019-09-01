package network

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Net
import com.badlogic.gdx.net.ServerSocket
import com.badlogic.gdx.net.Socket
import java.io.*
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

data class ClientSocket(val socket: Socket) {
    val output = DataOutputStream(socket.outputStream)
    val input = DataInputStream(socket.inputStream)
    val handlerThread: Thread

    init {
        handlerThread = thread(isDaemon = true) {
            println("Starting client thread")

            // sends output to the socket
            while (Server.running.get()) {
                // read int blocks until next input is sent
                val nextPacketType = input.readInt()
                if (nextPacketType == -1) {
                    break
                }
                try {
                    val newPacket = PacketType.read(nextPacketType, input)
                    Server.recievedPackets.add(newPacket)
                } catch (e: InvalidObjectException) {
                    println("Invalid packet received from client ${socket.remoteAddress}")
                }
            }
            output.close()
            input.close()
        }
    }
}

object Server : PacketHandler {
    val running = AtomicBoolean(false)

    lateinit var socket: ServerSocket

    val clientSockets = mutableListOf<ClientSocket>()
    lateinit var networkAddresses: List<String>

    lateinit var thread: Thread

    val recievedPackets = Collections.synchronizedList(mutableListOf<Packet>())

    private val packetHandlers = mutableMapOf<PacketHandler, List<PacketType>>()
    val outwardPackets = Collections.synchronizedList(mutableListOf<Packet>())

    fun start() {
        NetworkManager.server = true
        running.set(true)
        registerPacketHandler(this, PacketType.CLIENT_HANDSHAKE)
        findNetworkInterfaces()
        thread = thread(start = true, isDaemon = true) {
            socket = Gdx.net.newServerSocket(Net.Protocol.TCP, "127.0.0.1", 9412, null)
            println("Server started")
            while (running.get()) {
                val newClient = socket.accept(null)
                println("Client ${newClient.remoteAddress} connected")
                clientSockets.add(ClientSocket(newClient))
            }
        }
    }

    fun close() {
        running.set(false)
        synchronized(clientSockets) {
            for (client in clientSockets) {
                println("Closing connection to client ${client.socket.remoteAddress}")
                client.handlerThread.join()
            }
        }
        thread.join()
    }

    override fun handlePacket(packet: Packet) {
        if(packet is ClientHandshakePacket) {
            val currentTime = System.currentTimeMillis()
            sendToClients(ServerHandshakePacket(packet.timestamp, currentTime))
        }
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

    fun sendToClients(packet: Packet) {
        outwardPackets.add(packet)
    }

    fun update() {
        synchronized(recievedPackets) {
            for (packet in recievedPackets) {
                for ((handler, types) in packetHandlers) {
                    if (packet.type in types) {
                        handler.handlePacket(packet)
                    }
                }
            }
            recievedPackets.clear()
        }
        synchronized(outwardPackets) {
            for (packet in outwardPackets) {
                for (client in clientSockets) {
                    packet.write(client.output)
                    client.output.flush()
                }
            }
            outwardPackets.clear()
        }
    }
}
