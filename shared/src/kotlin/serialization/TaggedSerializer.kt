package serialization

import kotlin.reflect.jvm.kotlinProperty


/**
 * An implementation of [Serializer] that automatically reads and writes to fields marked with the [Id] annotation.
 * It also uses default constructor initialization--to change this, override the class.
 *
 * For example, if I have a class defined
 *
 *
 *      class Test {
 *          @Id(1)
 *          val integer = 0
 *      }
 *
 * Using this serializer on it will automatically write the value of `integer` to the stream when write is called, and read it back into
 * `integer` when read is called. This works with arbitrary types, not just [Int], of course so long as they have been
 * registered appropriately.
 *
 * This is usually a good choice when you don't know what serializer to use for an object, because it is relatively
 * strong as you make changes. What I mean by that is you can rename, reorder and even delete variables as you like, but as long as you
 * don't change the parameter of the [Id] annotation, this will be able to read it correctly.
 *
 * Keep in mind that any field that does not have an [Id] annotation will not be read or written to, i.e. it is invisible
 * to the serializer and is effectively transient
 */
open class TaggedSerializer<R : Any>(type: Class<R>, settings: List<SerializerSetting<*>>) :
    FieldSerializer<R>(type, settings) {

    // we have to make this lazy because if we don't, then FieldSerializer will call Registration.getSerializer
    // on classes that haven't been assigned a serializer yet and so it will have the wrong serializer.
    val taggedFields: Array<CachedField> by lazy {
        val tags = fields.filter { IdSetting in it.serializer.settings }.sortedBy { it.field.id }
            .toTypedArray()
        // make sure we don't have conflicting ids
        for (taggedField in tags) {
            val id = taggedField.field.id
            val alreadyExisting = tags.firstOrNull { it.field.id == id }
            if (alreadyExisting != null) {
                throw RegistrationException("Two fields in class $type have the same id ($id): ${taggedField.field.name} and ${alreadyExisting.field.name}")
            }
        }
        if (tags.isNotEmpty()) {
            SerializerDebugger.writeln("Found tagged fields:")
            for (field in tags) {
                SerializerDebugger.writeln("    $field")
            }
        } else {
            SerializerDebugger.writeln("Found no tagged fields")
        }
        tags
    }

    /**
     * Writes all fields in [type] that have been tagged with the annotation [Id] to the [output] stream. See [Serializer.write]
     * for more general details about this function
     */
    override val writeStrategy = object : WriteStrategy<R>(type) {
        override fun write(obj: R, output: Output) {
            // write size so that we know if the number has changed (possibly because something was changed, its class was edited, and it was reloaded again)
            output.writeInt(taggedFields.size)
            for (field in taggedFields) {
                synchronized(field) {
                    /*
                    if (!field.field.trySetAccessible()) {
                        throw WriteException("Unable to set accessibility of field ${field.field} from class ${obj::class} to true")
                    }
    */
                    field.field.isAccessible = true
                    try {
                        val fieldValue = field.field.get(obj)
                        SerializerDebugger.writeln("Writing tagged field $field = $fieldValue")
                        output.writeInt(field.field.id)
                        output.write(fieldValue, field.serializer as Serializer<Any>)
                    } catch (e: IllegalAccessException) {
                        System.err.println("Error while getting value of field ${field.field} from class ${obj::class} (accessible: ${field.field.isAccessible}")
                        throw e
                    }
                    field.field.isAccessible = false
                    // this is commented out because i think there are issues in regards to multi-threading that happen when
                    // this is set to false
                }
            }
        }
    }

    /**
     * Reads all fields in this [type] tagged with the annotation [Id], and sets the fields in [newInstance] to whatever
     * the values it reads are. See [Serializer.read] for more general details about this function
     */
    override val readStrategy = object : ReadStrategy<R>(type) {
        override fun read(obj: R, input: Input) {
            val supposedSize = input.readInt()
            if (supposedSize > taggedFields.size) {
                // there were more but now there are less, that is ok, no cause for crashing, we just need to make sure that
                // the stream ends up reading them all so that the next call to read doesn't read one of the parameters
            } else if (supposedSize < taggedFields.size) {
                // we don't want this because that means there will be a tagged field in the class that won't get assigned a value
                // i suppose this could be better handled by trying to create an instance of it with default constructor but that's just not a great idea
                throw ReadException("Encountered a tagged field in $type that is supposed to have only $supposedSize tagged fields according to the file, but actually has ${taggedFields.size}")
            }
            for (i in 0 until supposedSize) {
                val fieldId = input.readInt()
                val taggedField = taggedFields.firstOrNull { it.field.id == fieldId }
                val field = taggedField?.field
                if (field == null) {
                    // we found a field that exists in the file but not in our class
                    // read the next thing anyways so that we make a complete read of the object
                    SerializerDebugger.writeln("Found a tagged field @Id($fieldId) in the file that does not exist in the class")
                    val uselessValue = input.readUnknownNullable()
                    SerializerDebugger.writeln("It had a value of $uselessValue")
                    continue
                }
                synchronized(field) {
                    SerializerDebugger.writeln("Reading tagged field $field ")

                    // set the value of the tagged field by recursively deserializing it
                    val nullable = field.kotlinProperty?.returnType?.isMarkedNullable
                    val newValue: Any?
                    if (nullable == true) {
                        newValue = input.readNullable(field.type)
                    } else {
                        newValue = input.read(field.type)
                    }
                    /*
                    if (!field.trySetAccessible()) {
                        throw ReadException("Unable to set accessible of field $field from class ${newInstance::class}")
                    }
                     */
                    field.isAccessible = true
                    try {
                        field.set(obj, newValue)
                    } catch (e: IllegalAccessException) {
                        println("Error while setting field $field in ${obj::class} to $newValue: (accessible: ${field.isAccessible})")
                        throw e
                    }
                    field.isAccessible = false
                }

                // this is commented out because i think there are issues in regards to multi-threading that happen when
                // this is set to false
                // TODO will this cause security issues?
            }
        }
    }
}