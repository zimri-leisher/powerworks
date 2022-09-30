package serialization

import serialization.Registration.REFERENCE_ID
import serialization.Registration.getId
import serialization.Registration.getSerializer
import serialization.SerializerDebugger.alreadyPrinted
import serialization.SerializerDebugger.lastLinesBuffer
import serialization.SerializerDebugger.safe
import java.io.*
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.*

// TODO sanity checks on what gets read/written for security

class ReadException(message: String) : Exception(message)

class RegistrationException(message: String) : Exception(message)

class WriteException(message: String) : Exception(message)

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
        SerializerDebugger.writeln("Copying object $obj")
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
        val lambdaSuperClass = { 0 }::class.java.superclass
        when (type) {
            Function1::class.java, Function2::class.java, Function3::class.java,
            Function4::class.java, Function5::class.java, Function6::class.java -> return lambdaSuperClass
        }
        if (type.superclass == lambdaSuperClass) {
            return lambdaSuperClass
        }
        return type
    }

    fun isPrimitive(id: Int) = Primitive.values().any { it.id == id }
}