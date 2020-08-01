package network

import com.esotericsoftware.kryo.io.ByteBufferInput
import com.esotericsoftware.kryo.io.ByteBufferOutput
import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.Serialization
import main.Game
import serialization.Input
import serialization.Output
import java.nio.ByteBuffer

/**
 * A way of switching out KryoNet's default serialization, which is of course [Kryo], for our own
 */
class NetworkSerializationPlug : Serialization {
    private val inputByteBuffer: ByteBufferInput = ByteBufferInput(128)
    private val outputByteBuffer: ByteBufferOutput = ByteBufferOutput(128)
    private val input: Input
    private val output: Output

    init {
        input = Input(inputByteBuffer)
        output = Output(outputByteBuffer)
    }

    @Synchronized
    override fun write(connection: Connection?, buffer: ByteBuffer?, `object`: Any?) {
        synchronized(if (Game.IS_SERVER) ServerNetworkManager.handlingLock else ClientNetworkManager.handlingLock) {
            try {
                outputByteBuffer.setBuffer(buffer)
                output.write(`object`)
                outputByteBuffer.flush()
                output.clearReferences()
            } catch (e: Exception) {
                output.clearReferences()
                System.err.println("Exception in write packet")
                throw e
            }

        }
    }

    @Synchronized
    override fun read(connection: Connection?, buffer: ByteBuffer?): Any? {
        synchronized(if (Game.IS_SERVER) ServerNetworkManager.handlingLock else ClientNetworkManager.handlingLock) {
            inputByteBuffer.setBuffer(buffer)
            try {
                val obj = input.readUnknownNullable()
                input.clearReferences()
                return obj
            } catch (e: Exception) {
                input.clearReferences()
                System.err.println("Exception in read packet")
                throw e
            }
        }
    }

    override fun writeLength(buffer: ByteBuffer, length: Int) {
        buffer.putInt(length)
    }

    override fun readLength(buffer: ByteBuffer): Int {
        return buffer.getInt()
    }

    override fun getLengthLength(): Int {
        return 4
    }
}