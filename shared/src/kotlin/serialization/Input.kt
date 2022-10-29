package serialization

import java.io.DataInputStream
import java.io.InputStream
import java.lang.reflect.Modifier
import java.util.*

class Input(inputStream: InputStream) : DataInputStream(inputStream) {

    private val references = IdentityHashMap<Any, Int>(16384)

    private var nextReferenceId = 0

    fun <R : Any> instantiate(type: Class<R>, useSerializer: Serializer<out Any>? = null): R {
        SerializerDebugger.writeln("-- Begin instantiating $type")
        SerializerDebugger.increaseDepth()
        val serializer = useSerializer ?: Registration.getSerializer(type)
        val instance = SerializerDebugger.catchAndPrintIfSafe { serializer.createStrategy.create(this) as R }
        SerializerDebugger.decreaseDepth()
        SerializerDebugger.writeln("-- End instantiating $type = $instance")
        return instance
    }

    private fun readPrimitive(id: Int): Any? {
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
        SerializerDebugger.writeln("Read primitive ${if (prim == null) null else prim::class.java} = $prim")
        return prim
    }

    private fun reserveReference() = nextReferenceId++

    private fun addReference(obj: Any, id: Int) {
        SerializerDebugger.writeln("(reference id: $id)")
        references.put(obj, id)
    }

    private fun readReference(): Any {
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
        SerializerDebugger.decreaseDepth()
        SerializerDebugger.writeln("-- End read of reference id $referenceId")
        return value
    }

    fun readUnknown(settings: Set<SerializerSetting<*>> = setOf()): Any {
        SerializerDebugger.writeln("-- Begin read of unknown non-null ${settings.joinToString()}")
        SerializerDebugger.increaseDepth()
        val supposedClassId = readUnsignedShort()
        if (Serialization.isPrimitive(supposedClassId)) {
            val prim = readPrimitive(supposedClassId)
                ?: throw ReadException("Encountered null value when trying to read non-null value of unknown class")
            SerializerDebugger.decreaseDepth()
            SerializerDebugger.writeln("-- End read of unknown non-null = $prim")
            return prim
        }
        if (supposedClassId == Registration.REFERENCE_ID) {
            SerializerDebugger.writeln("(Value is a reference)")
            return SerializerDebugger.catchAndPrintIfSafe { readReference() }
        }
        val supposedClassType = Registration.getType(supposedClassId)
            ?: throw ReadException("Unregistered class id $supposedClassId encountered while trying to read value of unknown class")
        SerializerDebugger.writeln("Found class id $supposedClassId -> $supposedClassType")
        return SerializerDebugger.catchAndPrintIfSafe { unsafeRead(supposedClassType, settings) }
    }

    fun readUnknownNullable(settings: Set<SerializerSetting<*>> = setOf()): Any? {
        SerializerDebugger.writeln("-- Begin read of unknown nullable ${settings.joinToString()}")
        SerializerDebugger.increaseDepth()
        val supposedClassId = readUnsignedShort()
        if (Serialization.isPrimitive(supposedClassId)) {
            val prim = readPrimitive(supposedClassId)
            SerializerDebugger.decreaseDepth()
            SerializerDebugger.writeln("-- End read of unknown nullable = $prim")
            return prim
        }
        if (supposedClassId == Registration.REFERENCE_ID) {
            SerializerDebugger.writeln("(Value is a reference)")
            return SerializerDebugger.catchAndPrintIfSafe { readReference() }
        }
        val supposedClassType = Registration.getType(supposedClassId)
            ?: throw ReadException("Unregistered class id $supposedClassId encountered while trying to read value of unknown class")
        SerializerDebugger.writeln("Found class id $supposedClassId -> $supposedClassType")
        return SerializerDebugger.catchAndPrintIfSafe { unsafeRead(supposedClassType, settings) }
    }

    fun <R> readNullable(type: Class<R>, settings: Set<SerializerSetting<*>> = setOf()): R? {
        SerializerDebugger.writeln("-- Begin read of potentially null $type ${settings.joinToString()}")
        SerializerDebugger.increaseDepth()
        val actualType = makeTypeNice(type)
        val supposedClassId = readUnsignedShort()
        if (supposedClassId == Primitive.NULL.id) {
            SerializerDebugger.decreaseDepth()
            SerializerDebugger.writeln("-- End read of potentially null $type = null")
            return null
        }
        if (supposedClassId == Registration.REFERENCE_ID) {
            SerializerDebugger.writeln("(Value is a reference)")
            return SerializerDebugger.catchAndPrintIfSafe { readReference() as R }
        }
        val supposedClassType = verifyType(actualType, supposedClassId)
        if (Serialization.isPrimitive(supposedClassId)) {
            val prim = readPrimitive(supposedClassId) as R
            SerializerDebugger.decreaseDepth()
            SerializerDebugger.writeln("-- End read of potentially null $type = $prim")
            return prim
        }
        return SerializerDebugger.catchAndPrintIfSafe { unsafeRead(supposedClassType, settings) as R }
    }

    fun <R> read(type: Class<R>, settings: Set<SerializerSetting<*>> = setOf()): R {
        SerializerDebugger.writeln("-- Begin read of non-null $type ${settings.joinToString()}")
        SerializerDebugger.increaseDepth()
        val actualType = makeTypeNice(type)
        val supposedClassId = readUnsignedShort()
        if (supposedClassId == Primitive.NULL.id) {
            throw ReadException("Encountered null value when trying to read non-null class $actualType")
        }
        if (supposedClassId == Registration.REFERENCE_ID) {
            SerializerDebugger.writeln("(Value is a reference to a previously serialized value)")
            return SerializerDebugger.catchAndPrintIfSafe { readReference() as R }
        }
        val supposedClassType = verifyType(actualType, supposedClassId)
        if (Serialization.isPrimitive(supposedClassId)) {
            val prim = readPrimitive(supposedClassId) as R
            SerializerDebugger.decreaseDepth()
            SerializerDebugger.writeln("-- End read of non-null $type = $prim")
            return prim
        }
        return SerializerDebugger.catchAndPrintIfSafe { unsafeRead(supposedClassType, settings) as R }
    }

    private fun makeTypeNice(type: Class<*>): Class<*> {
        val nonPrimitiveType = Serialization.tryConvertPrimitiveToWrapper(type)
        if (nonPrimitiveType != type) {
            SerializerDebugger.writeln("(Converted type from Java primitive type to Java wrapper primitive type)")
        }
        val nonSpecificArrayType = Serialization.tryDetypeArray(nonPrimitiveType)
        if (nonSpecificArrayType != nonPrimitiveType) {
            SerializerDebugger.writeln("(Converted type from a specific array to generic Object array)")
        }
        val nonSpecificLambdaType = Serialization.tryGetLambdaClass(nonSpecificArrayType)
        if (nonSpecificLambdaType != nonSpecificArrayType) {
            SerializerDebugger.writeln("(Converted type from a specific lambda to a generic one)")
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

    private fun unsafeRead(type: Class<*>, settings: Set<SerializerSetting<*>>): Any {
        SerializerDebugger.writeln("Reading non-primitive $type ${settings.joinToString()}")
        // we want to reserve a reference id for the instance before we instantiate it, because in instantiation it could create
        // new references
        val referenceId = reserveReference()
        val serializer = Registration.getSerializer(type, settings)
        val instance = instantiate(type, serializer)
        addReference(instance, referenceId)
        (serializer.readStrategy as ReadStrategy<Any>).read(instance, this)
        SerializerDebugger.decreaseDepth()
        SerializerDebugger.writeln("-- End read of $type = $instance")
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

