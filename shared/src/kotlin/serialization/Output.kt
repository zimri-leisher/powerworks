package serialization

import java.io.DataOutputStream
import java.io.OutputStream
import java.lang.reflect.Field
import java.util.*

class Output(outputStream: OutputStream) : DataOutputStream(outputStream) {

    private val references = IdentityHashMap<Any, Int>(512)

    private var nextReferenceId = 0

    private fun addReference(obj: Any): Int {
        val id = nextReferenceId++
        SerializerDebugger.writeln("(reference id: $id)")
        references.put(obj, id)
        return id
    }

    private fun makeTypeNice(type: Class<*>): Class<*> {
        val lambdaSuperClass = { 0 }::class.java.superclass
        if (type.superclass == lambdaSuperClass || type.superclass == Function::class.java) {
            return lambdaSuperClass
        }
        return type
    }

    private fun writeReference(reference: Int) {
        writeShort(Registration.REFERENCE_ID)
        writeInt(reference)
    }

    private fun writePrimitive(id: Int, prim: Any) {
        writeShort(id)
        when (id) {
            Primitive.BOOLEAN.id -> writeBoolean(prim as Boolean)
            Primitive.DOUBLE.id -> writeDouble(prim as Double)
            Primitive.BYTE.id -> writeByte((prim as Byte).toInt())
            Primitive.NULL.id -> writeShort(Primitive.NULL.id)
            Primitive.CHAR.id -> writeChar((prim as Char).toInt())
            Primitive.INT.id -> writeInt(prim as Int)
            Primitive.LONG.id -> writeLong(prim as Long)
            Primitive.SHORT.id -> writeShort((prim as Short).toInt())
            Primitive.FLOAT.id -> writeFloat(prim as Float)
            Primitive.STRING.id -> writeUTF(prim as String)
        }
        SerializerDebugger.writeln("Written primitive")
    }

    fun write(obj: Any?, useSerializer: Serializer<Any>? = null) {
        SerializerDebugger.writeln("-- Begin writing ${if (obj == null) null else obj::class.java} = $obj")
        SerializerDebugger.increaseDepth()
        if (obj == null) {
            writeShort(Primitive.NULL.id)
            SerializerDebugger.decreaseDepth()
            SerializerDebugger.writeln("-- End writing null")
            return
        }
        val niceType = makeTypeNice(obj::class.java)
        val id = Registration.getId(niceType)
        if (Serialization.isPrimitive(id)) {
            writePrimitive(id, obj)
        } else {
            writeNonNullNonPrimitive(id, niceType, obj, useSerializer ?: Registration.getSerializer(obj::class.java))
        }
        SerializerDebugger.decreaseDepth()
        SerializerDebugger.writeln("-- End writing $niceType = $obj")
    }

    private fun writeNonNullNonPrimitive(id: Int, niceType: Class<*>, obj: Any, serializer: Serializer<Any>) {
        val reference = references.get(obj)
        if (reference != null) {
            SerializerDebugger.writeln("(Object has been written before)")
            writeReference(reference)
            SerializerDebugger.decreaseDepth()
            SerializerDebugger.writeln("-- End writing $niceType = $obj as internal reference $reference")
            return
        }
        addReference(obj)
        writeShort(id)
        serializer.writeStrategy.write(obj, this)
    }

    fun clearReferences() {
        nextReferenceId = 0
        references.clear()
    }

    override fun close() {
        nextReferenceId = 0
        references.clear()
        super.close()
    }
}