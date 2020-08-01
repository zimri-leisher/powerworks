package serialization

import serialization.Registration.REFERENCE_ID
import serialization.Registration.getId
import serialization.Registration.getRegistry
import java.io.*
import java.lang.reflect.Modifier
import java.util.*

class ReadException(message: String) : Exception(message)

class RegistrationException(message: String) : Exception(message)

class WriteException(message: String) : Exception(message)

fun debugln(text: String) {
    SerializerDebugger.writeln(text)
}

private fun increaseDepth() {
    SerializerDebugger.debugDepth++
}

private fun decreaseDepth() {
    SerializerDebugger.debugDepth--
}

object SerializerDebugger {

    const val on = false
    var debugDepth = 0
    val debugSpaces get() = (0 until (debugDepth % 16)).joinToString { "   " }

    fun writeln(text: String) {
        if (on) {
            println("[Serializer] $debugSpaces $text")
        }
    }
}

object Serialization {

    class WarmupObject() {
        @Id(1)
        val warmupCircularVal = this

        @Id(2)
        val warmupVal = 0
    }

    fun warmup() {
        val byteOutput = ByteArrayOutputStream(1028)
        val output = Output(byteOutput)
        output.write(WarmupObject())
        output.close()
        val input = Input(ByteArrayInputStream(byteOutput.toByteArray()))
        input.readUnknownNullable()
        input.close()
    }

    val copyBuffer = ByteArrayOutputStream(512)
    val copyOutputStream = Output(copyBuffer)

    fun <T> copy(obj: T): T {
        debugln("Copying object $obj")
        copyOutputStream.write(obj)
        val input = Input(ByteArrayInputStream(copyBuffer.toByteArray()))
        copyBuffer.reset()
        copyOutputStream.clearReferences()
        return input.readUnknownNullable() as T
    }

    fun makeTypeNice(type: Class<*>) = tryGetLambdaClass(tryConvertPrimitiveToWrapper(tryDetypeArray(type)))

    fun tryConvertPrimitiveToWrapper(type: Class<*>): Class<*> {
        return when (type) {
            Integer::class.javaPrimitiveType -> Integer::class.javaObjectType
            Byte::class.javaPrimitiveType -> Byte::class.javaObjectType
            Short::class.javaPrimitiveType -> Short::class.javaObjectType
            Float::class.javaPrimitiveType -> Float::class.javaObjectType
            Double::class.javaPrimitiveType -> Double::class.javaObjectType
            Char::class.javaPrimitiveType -> Char::class.javaObjectType
            Boolean::class.javaPrimitiveType -> Boolean::class.javaObjectType
            Long::class.javaPrimitiveType -> Long::class.javaObjectType
            else -> type
        }
    }

    fun tryDetypeArray(type: Class<*>): Class<*> {
        if (type.isArray) {
            return Array<out Any?>::class.java
        }
        return type
    }

    fun tryGetLambdaClass(type: Class<*>): Class<*> {
        val lambdaSuperClass = {0}::class.java.superclass
        when(type) {
            Function1::class.java, Function2::class.java, Function3::class.java,
            Function4::class.java, Function5::class.java, Function6::class.java -> return lambdaSuperClass
        }
        if(type.superclass == lambdaSuperClass) {
            return lambdaSuperClass
        }
        return type
    }

    fun isPrimitive(id: Int) = Primitive.values().any { it.id == id }
}

class Input(inputStream: InputStream) : DataInputStream(inputStream) {

    private val references = IdentityHashMap<Any, Int>(16384)

    private var nextReferenceId = 0

    fun <R : Any> instantiate(type: Class<R>): R {
        debugln("-- Begin instantiating $type")
        increaseDepth()
        val registry = getRegistry(type) as ClassRegistry<R>
        val instance = registry.serializer.instantiate(this) as R
        decreaseDepth()
        debugln("-- End instantiating $type = $instance")
        return instance
    }

    fun readPrimitive(id: Int): Any? {
        val prim: Any? = when (id) {
            Primitive.BOOLEAN.id -> readBoolean()
            Primitive.DOUBLE.id -> readDouble()
            Primitive.BYTE.id -> readByte()
            Primitive.NULL.id -> null
            Primitive.CHAR.id -> readChar()
            Primitive.INT.id -> readInt()
            Primitive.LONG.id -> readLong()
            Primitive.SHORT.id -> readShort()
            Primitive.FLOAT.id -> readFloat()
            Primitive.STRING.id -> readUTF()
            else -> null
        }
        debugln("Read primitive ${if (prim == null) null else prim::class.java} = $prim")
        return prim
    }

    private fun reserveReference() = nextReferenceId++

    private fun addReference(obj: Any, id: Int) {
        debugln("(reference id: $id)")
        references.put(obj, id)
    }

    fun readReference(): Any {
        val referenceId = readInt()
        val objectsReferencedById = mutableListOf<Any>()
        references.forEach { (key, value) ->
            if (value == referenceId) {
                objectsReferencedById.add(key)
            }
        }
        val value: Any
        if (objectsReferencedById.size > 1) {
            throw ReadException("More than 1 object is referenced by id $referenceId!")
        } else if (objectsReferencedById.isNotEmpty()) {
            value = objectsReferencedById.first()
        } else {
            throw ReadException("A reference with id $referenceId was read, but none exists in the graph (at this point in reading)")
        }
        decreaseDepth()
        debugln("-- End read of reference id $referenceId")
        return value
    }

    fun readUnknown(): Any {
        debugln("-- Begin read of unknown non-null")
        increaseDepth()
        val supposedClassId = readUnsignedShort()
        if (Serialization.isPrimitive(supposedClassId)) {
            val prim = readPrimitive(supposedClassId)
                    ?: throw ReadException("Encountered null value when trying to read non-null value of unknown class")
            decreaseDepth()
            debugln("-- End read of unknown non-null = $prim")
            return prim
        }
        if (supposedClassId == REFERENCE_ID) {
            debugln("(Value is a reference)")
            return readReference()
        }
        val supposedClassType = Registration.getType(supposedClassId)
                ?: throw ReadException("Unregistered class id $supposedClassId encountered while trying to read value of unknown class")
        debugln("Found class id $supposedClassId -> $supposedClassType")
        return unsafeRead(supposedClassType)
    }

    fun readUnknownNullable(): Any? {
        debugln("-- Begin read of unknown nullable")
        increaseDepth()
        val supposedClassId = readUnsignedShort()
        if (Serialization.isPrimitive(supposedClassId)) {
            val prim = readPrimitive(supposedClassId)
            decreaseDepth()
            debugln("-- End read of unknown nullable = $prim")
            return prim
        }
        if (supposedClassId == REFERENCE_ID) {
            debugln("(Value is a reference)")
            return readReference()
        }
        val supposedClassType = Registration.getType(supposedClassId)
                ?: throw ReadException("Unregistered class id $supposedClassId encountered while trying to read value of unknown class")
        debugln("Found class id $supposedClassId -> $supposedClassType")
        return unsafeRead(supposedClassType)
    }

    fun <R> readNullable(type: Class<R>): R? {
        debugln("-- Begin read of potentially null $type")
        increaseDepth()
        val actualType = makeTypeNice(type)
        val supposedClassId = readUnsignedShort()
        if (supposedClassId == Primitive.NULL.id) {
            decreaseDepth()
            debugln("-- End read of potentially null $type = null")
            return null
        }
        if (supposedClassId == REFERENCE_ID) {
            debugln("(Value is a reference)")
            return readReference() as R
        }
        val supposedClassType = verifyType(actualType, supposedClassId)
        if (Serialization.isPrimitive(supposedClassId)) {
            val prim = readPrimitive(supposedClassId) as R
            decreaseDepth()
            debugln("-- End read of potentially null $type = $prim")
            return prim
        }
        return unsafeRead(supposedClassType) as R
    }

    fun <R> read(type: Class<R>): R {
        debugln("-- Begin read of non-null $type")
        increaseDepth()
        val actualType = makeTypeNice(type)
        val supposedClassId = readUnsignedShort()
        if (supposedClassId == Primitive.NULL.id) {
            throw ReadException("Encountered null value when trying to read non-null class $actualType")
        }
        if (supposedClassId == REFERENCE_ID) {
            debugln("(Value is a reference)")
            return readReference() as R
        }
        val supposedClassType = verifyType(actualType, supposedClassId)
        if (Serialization.isPrimitive(supposedClassId)) {
            val prim = readPrimitive(supposedClassId) as R
            decreaseDepth()
            debugln("-- End read of non-null $type = $prim")
            return prim
        }
        return unsafeRead(supposedClassType) as R
    }

    private fun makeTypeNice(type: Class<*>): Class<*> {
        val nonPrimitiveType = Serialization.tryConvertPrimitiveToWrapper(type)
        if (nonPrimitiveType != type) {
            debugln("(Converted type from Java primitive type to Java wrapper primitive type)")
        }
        val nonSpecificArrayType = Serialization.tryDetypeArray(nonPrimitiveType)
        if (nonSpecificArrayType != nonPrimitiveType) {
            debugln("(Converted type from a specific array to generic Object array)")
        }
        val nonSpecificLambdaType = Serialization.tryGetLambdaClass(nonSpecificArrayType)
        if(nonSpecificLambdaType != nonSpecificArrayType) {
            debugln("(Converted type from a specific lambda to a generic one)")
        }
        return nonSpecificLambdaType
    }

    private fun verifyType(desiredType: Class<*>, supposedClassId: Int): Class<*> {
        val supposedClassType = Registration.getType(supposedClassId)
        if (supposedClassType == null) {
            throw ReadException("Unregistered class id $supposedClassId encountered while trying to read $desiredType")
        } else if (desiredType != supposedClassType) {
            if (Modifier.isAbstract(desiredType.modifiers) || desiredType.isInterface) {
                // the actual type we are trying to read is an abstract class or interface
                // in this situation the supposedClassType should be a child class of the supertype
                if (!desiredType.isAssignableFrom(supposedClassType)) {
                    // verify that it is
                    throw ReadException("Class type in file ($supposedClassType) does not match class trying to be read ${desiredType}")
                }
            }
        }
        return supposedClassType
    }

    // when reading an array, first we create the inner objects, then we create the overall array instance
    // thus we first give references to the inner objects, and then to the array instance
    // when writing, we give a reference id to the array first, and then its inner classes

    private fun unsafeRead(type: Class<*>): Any {
        debugln("Reading non-primitive $type")
        // we want to reserve a reference id for the instance before we instantiate it, because in instantiation it could create
        // new references
        val referenceId = reserveReference()
        val instance = instantiate(type)
        addReference(instance, referenceId)
        val registry = getRegistry(type)
        registry.serializer.read(instance, this)
        decreaseDepth()
        debugln("-- End read of $type = $instance")
        return instance
    }

    fun clearReferences() {
        nextReferenceId = 0
        references.clear()
    }

    override fun close() {
        references.clear()
        nextReferenceId = 0
        super.close()
    }
}

class Output(outputStream: OutputStream) : DataOutputStream(outputStream) {

    private val references = IdentityHashMap<Any, Int>(512)

    private var nextReferenceId = 0

    private fun addReference(obj: Any): Int {
        val id = nextReferenceId++
        debugln("(reference id: $id)")
        references.put(obj, id)
        return id
    }

    private fun makeTypeNice(type: Class<*>): Class<*> {
        val lambdaSuperClass = {0}::class.java.superclass
        if(type.superclass == lambdaSuperClass || type.superclass == Function::class.java) {
            return lambdaSuperClass
        }
        return type
    }

    fun write(obj: Any?) {
        debugln("-- Begin writing ${if (obj == null) null else obj::class.java} = $obj")
        increaseDepth()
        if (obj == null) {
            writeShort(Primitive.NULL.id)
            decreaseDepth()
            debugln("-- End writing null")
            return
        }
        val niceType = makeTypeNice(obj::class.java)
        val id = getId(niceType)
        if (Serialization.isPrimitive(id)) {
            writePrimitive(obj)
        } else {
            val reference = references.get(obj)
            if (reference != null) {
                debugln("(Object has been written before)")
                writeReference(reference)
                decreaseDepth()
                debugln("-- End writing ${niceType} = $obj as reference $reference")
                return
            }
            val registry = getRegistry(niceType)
            addReference(obj)
            writeShort(id)
            registry.serializer.write(obj, this)
        }
        decreaseDepth()
        debugln("-- End writing ${niceType} = $obj")
    }

    private fun writeReference(reference: Int) {
        writeShort(REFERENCE_ID)
        writeInt(reference)
    }

    fun writePrimitive(prim: Any) {
        val id = getId(prim::class.java)
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
        debugln("Written primitive")
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