package serialization

import java.lang.reflect.Field
import kotlin.reflect.jvm.kotlinProperty

/**
 * This is exactly like a [TaggedSerializer] except no annotations are necessary, instead, it saves all fields. Instead
 * of using the parameter of [Id] as an identifier, it saves them by name. Note this is brittle if you save an object,
 * rename one of its fields, and then reload it.
 */
open class AllFieldsSerializer<T : Any>(type: Class<T>, settings: List<SerializerSetting<*>>) :
    FieldSerializer<T>(type, settings) {

    init {
        if (fields.isNotEmpty()) {
            SerializerDebugger.writeln("Fields:")
            for (cached in fields) {
                SerializerDebugger.writeln("    ${cached.field.name}: ${cached.field.type.simpleName}")
            }
        } else {
            SerializerDebugger.writeln("No fields found.")
        }
    }

    override val writeStrategy = object : WriteStrategy<T>(type, settings) {
        override fun write(obj: T, output: Output) {
            SerializerDebugger.writeln("Writing number of fields: ${fields.size}")
            output.writeInt(fields.size)
            for (field in fields) {
                synchronized(field) {
                    /*
                    if (!field.trySetAccessible()) {
                        throw WriteException("Unable to set accessible of field $field from class ${obj::class}")
                    }
                     */
                    field.field.isAccessible = true
                    val fieldValue = field.field.get(obj)
                    SerializerDebugger.writeln("Writing field ${field.field.name}: ${field.field.type.simpleName} = $fieldValue")
                    output.writeUTF(field.field.name)
                    output.write(fieldValue, field.settings)
                    field.field.isAccessible = false
                }
            }
        }
    }

    override val readStrategy = object : ReadStrategy<T>(type, settings) {
        override fun read(obj: T, input: Input) {
            val supposedSize = input.readInt()
            SerializerDebugger.writeln("Supposed number of fields: $supposedSize")
            if (supposedSize < fields.size) {
                throw ReadException("Instance in file does not have enough fields to cover all of class $type ($supposedSize in file vs ${fields.size} needed)")
            } else if (supposedSize > fields.size) {
                // if it is bigger, just with TaggedSerializer, no cause for crashing, we just need to make sure that
                // this reads all the fields and doesn't leave the next one to be discovered by the next called of
                // input.read
            }
            // collect all name/value pairs
            val nameToValue = mutableMapOf<String, Any?>()
            for (i in 0 until supposedSize) {
                val fieldName = input.readUTF()
                SerializerDebugger.writeln("Found field with name $fieldName")
                var existingField: CachedField? = null
                for (field in fields) {
                    if (field.field.name == fieldName) {
                        existingField = field
                        break
                    }
                }
                val fieldValue = input.readUnknownNullable(existingField?.settings ?: listOf())
                nameToValue.put(fieldName, fieldValue)
            }

            for ((name, value) in nameToValue) {
                val field = fields.firstOrNull { it.field.name == name }
                if (field == null) {
                    // the field existed in a previous version of the class but no longer does,
                    // that is ok because that just means it is no longer necessary
                    SerializerDebugger.writeln("Found a field $name in the file that does not exist in the class $type")
                    SerializerDebugger.writeln("It had a value of $value")
                    continue
                }
                synchronized(field) {
                    SerializerDebugger.writeln("Reading field ${field.field.name}: ${field.field.type.simpleName} ")
                    field.field.isAccessible = true
                    /*
                    if (!field.trySetAccessible()) {
                        throw ReadException("Unable to set accessible of field $field from class ${newInstance::class}")
                    }

                     */
                    // set the value of the tagged field by recursively deserializing it
                    val nullable = field.field.kotlinProperty?.returnType?.isMarkedNullable
                    if (nullable == false && value == null) {
                        throw ReadException("Read a null value on a non-nullable field $name")
                    }
                    field.field.set(obj, value)
                    field.field.isAccessible = false
                }
            }
        }
    }
}