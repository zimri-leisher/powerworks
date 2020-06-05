package serialization

import item.ItemType
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import kotlin.reflect.jvm.kotlinProperty

/**
 * An annotation that tells [Serializer.Tagged] what id to give this field when reading and writing it. There are no
 * constraints on [id]
 */
@Target(AnnotationTarget.FIELD)
annotation class Id(val id: Int)

/**
 * A serializer for reading and writing of arbitrary objects of type [R]. One can override this class and its methods to
 * implement custom reading and writing behavior. To be clear, this only has default behavior for instantiation, and read/write
 * do nothing.
 *
 * To set a class type to use a serializer, input the serializer as an argument to any of the [Registration.register] methods.
 *
 * If [useDefaultConstructorInstantiation] is true, two things will happen. One,
 * the [onChangeType] method will find and cache an appropriate default [Constructor] for the [type], and two, the [instantiate] method will
 * use that default constructor to create new instances of the class when it is called. See the method specific documentation
 * for more details
 *
 * @param useDefaultConstructorInstantiation whether or not to use default constructor initialization. If true, instances of
 * the class will be obtained by calling, via reflection, a constructor with no arguments inside of the class. The constructor need
 * not be public, as it will be modified by reflection to be accessible
 */
open class Serializer<R : Any>(var useDefaultConstructorInstantiation: Boolean = false) {

    private lateinit var cachedConstructor: Constructor<R>

    /**
     * The type that this serializer is able to serialize. This should always end up being a concrete class. Note this is
     * not generically type-safe, e.g. even if this class is an instance of Serializer<Robot>, you can still set [type] equal
     * to a Class<LevelInfo>
     */
    var type: Class<*> = Any::class.java
        set(value) {
            if (field != value) {
                field = value
                onChangeType()
            }
        }

    /**
     * When the [type] variable is changed. This is called when a class uses any of the [Registration.register] methods
     * and this [Serializer], so you should really never have to manually call this method. Use it to create caches of
     * fields for the type and things like that.
     *
     * If you want [useDefaultConstructorInstantiation] to function properly, overrides of this method must call `super.onChangeType()`
     */
    open fun onChangeType() {
        if (useDefaultConstructorInstantiation) {
            if (type.isInterface || Modifier.isAbstract(type.modifiers)) {
                // default won't happen with either of these because for any instance of these the actual type will be something else
                return
            }
            val defaultConstructor = type.declaredConstructors.firstOrNull { it.parameterCount == 0 }
            if (defaultConstructor == null) {
                debugln("(No default (0-arg) constructor, checking if $type is member class)")
                if (type.isMemberClass) {
                    debugln("($type is member class)")
                    // needs an instance of superclass to instantiate
                    cachedConstructor = type.declaredConstructors.firstOrNull { it.parameterCount == 1 } as Constructor<R>?
                            ?: throw ReadException("Member class $type has no default constructor")
                    if (!cachedConstructor.isAccessible) {
                        cachedConstructor.isAccessible = true
                    }
                    return
                } else {
                    throw ReadException("Class $type has no default constructor")
                }
            }
            if (!defaultConstructor.isAccessible) {
                defaultConstructor.isAccessible = true
            }
            cachedConstructor = defaultConstructor as Constructor<R>
        }
    }

    /**
     * Given an input stream [input], this function should return an instance of class [type]. If a class needs to be serialized
     * but cannot have a default (0 argument) constructor, one should override this method and use the [input] stream to
     * read in appropriate arguments for its constructor.
     *
     * If you wish to use default constructor initialization but still want to override this method, make sure to call
     * `super.instantiate(input)` inside of the override.
     *
     * In the case that you are overriding this method to simply read some single value from [input] and return an already
     * existing instance of class [type] that is stored in a list somewhere, you can use the [IDSerializer] instead, which makes
     * that significantly more easy. For more details see the documentation for [IDSerializer]
     */
    open fun instantiate(input: Input): R {
        if (!useDefaultConstructorInstantiation) {
            throw ReadException("No instantiation function defined for type $type")
        }
        if (cachedConstructor.parameterCount == 1) {
            val superClass = type.enclosingClass
            debugln("(Using default (0-arg) constructor instantiation)")
            val superClassInstance = input.instantiate(superClass)
            return cachedConstructor.newInstance(superClassInstance) as R
        }
        return cachedConstructor.newInstance() as R
    }

    /**
     * Writes the entire [obj] to the [output] stream. [obj] is guaranteed to be an instance of [type], so casting it to [R] is safe.
     * It's not defined as `obj: R` here because I am not good enough with generics.
     *
     * For all objects which [java.io.DataOutputStream] has a specific method for (e.g. `writeInt`, `writeUTF` which writes [String]s,
     * `writeFloat`, etc.), it is best practice to call these directly on [output] instead of calling the generic [Output.write] function,
     * which is slightly more performance heavy for those basic types.
     *
     */
    open fun write(obj: Any, output: Output) {
    }

    /**
     * Reads fields from [input] into the given [newInstance], in theory completely finishing reading all of [newInstance].
     *
     * The [newInstance] instance is obtained from [instantiate]. This means that if the class can be completely read
     * just through inputting arguments into the constructor, it should have already been done in [instantiate] and this
     * function can be empty.
     *
     * Note it does not `return` anything. Instead, all reading should be done 'into' [newInstance].
     *
     * For all objects which [java.io.DataInputStream] has a specific method for (e.g. `readInt`, `readUTF` which reads [String]s,
     * `readFloat`, etc.), it is best practice to call these directly on [input] instead of calling the generic [Input.read] function,
     * which is slightly more performance heavy for those basic types.
     *
     */
    open fun read(newInstance: Any, input: Input) {
    }

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
    open class Tagged<R : Any>(useDefaultConstructorInstantiation: Boolean = true) : Serializer<R>(useDefaultConstructorInstantiation) {

        protected data class TaggedField(val field: Field, val id: Int)

        protected var cachedTaggedFields = listOf<TaggedField>()

        /**
         * Finds and caches fields in the [type] necessary for this [Serializer.Tagged]
         */
        override fun onChangeType() {

            fun getFields(type: Class<*>): Array<Field> {
                var fields = type.declaredFields
                if (type.superclass != null) {
                    fields += getFields(type.superclass)
                }
                return fields
            }

            val fields = getFields(type)
            val taggedFields = fields.filter { it.isAnnotationPresent(Id::class.java) }
            val cachedTaggedFieldsTemp = mutableListOf<TaggedField>()
            for(taggedField in taggedFields) {
                val id = taggedField.getAnnotation(Id::class.java).id
                if(cachedTaggedFieldsTemp.any { it.id == id }) {
                    throw RegistrationException("Two fields in class $type have the same id ($id)")
                }
                cachedTaggedFieldsTemp.add(TaggedField(taggedField, id))
            }
            cachedTaggedFields = cachedTaggedFieldsTemp
            if (cachedTaggedFields.isNotEmpty()) {
                debugln("Found tagged fields:")
                for (field in cachedTaggedFields) {
                    debugln("    @Id(${field.id}) ${field.field.name}: ${field.field.type.simpleName} ")
                }
            } else {
                debugln("Found no tagged fields")
            }

            super.onChangeType()
        }

        /**
         * Writes all fields in [type] that have been tagged with the annotation [Id] to the [output] stream. See [Serializer.write]
         * for more general details about this function
         */
        override fun write(obj: Any, output: Output) {
            obj as R
            // write size so that we know if the number has changed (possibly because something was changed, its class was edited, and it wsa reloaded again)
            output.writeInt(cachedTaggedFields.size)
            for (field in cachedTaggedFields) {
                var wasInaccessible = false
                if (!field.field.isAccessible) {
                    wasInaccessible = true
                    field.field.isAccessible = true
                }
                val fieldValue = field.field.get(obj)
                debugln("Writing tagged field @Id(${field.id}) ${field.field.name}: ${field.field.type.simpleName} = ${fieldValue}")
                output.writeInt(field.id)
                output.write(fieldValue)
                if (wasInaccessible) {
                    field.field.isAccessible = false
                }
            }
        }

        /**
         * Reads all fields in this [type] tagged with the annotation [Id], and sets the fields in [newInstance] to whatever
         * the values it reads are. See [Serializer.read] for more general details about this function
         */
        override fun read(newInstance: Any, input: Input) {
            newInstance as R
            val supposedSize = input.readInt()
            if (supposedSize > cachedTaggedFields.size) {
                // there were more but now there are less, that is ok, no cause for crashing, we just need to make sure that
                // the stream ends up reading them all so that the next call to read doesn't read one of the parameters
            } else if (supposedSize < cachedTaggedFields.size) {
                // we don't want this because that means there will be a tagged field in the class that won't get assigned a value
                // i suppose this could be better handled by trying to create an instance of it with default constructor but that's just not a great idea
                throw ReadException("Encountered a tagged field in $type that is supposed to have only $supposedSize tagged fields according to the file, but actually has ${cachedTaggedFields.size}")
            }
            for (i in 0 until supposedSize) {
                val fieldId = input.readInt()
                val field = cachedTaggedFields.firstOrNull { it.id == fieldId }?.field
                if (field == null) {
                    // we found a field that exists in the file but not in our class
                    // read the next thing anyways so that we make a complete read of the object
                    debugln("Found a tagged field @Id($fieldId) in the file that does not exist in the class")
                    val uselessValue = input.readUnknownNullable()
                    debugln("It had a value of $uselessValue")
                    continue
                }
                debugln("Reading tagged field @Id($fieldId) ${field.name}: ${field.type.simpleName} ")
                var wasInaccessible = false
                if (!field.isAccessible) {
                    wasInaccessible = true
                    field.isAccessible = true
                }
                // set the value of the tagged field by recursively deserializing it
                val nullable = field.kotlinProperty?.returnType?.isMarkedNullable
                val newValue: Any?
                if (nullable == true) {
                    newValue = input.readNullable(field.type)
                } else {
                    newValue = input.read(field.type)
                }
                field.set(newInstance, newValue)
                if (wasInaccessible) {
                    field.isAccessible = false
                }
            }
        }
    }

    /**
     * This is exactly like a [Serializer.Tagged] except no annotations are necessary, instead, it saves all fields. Instead
     * of using the parameter of [Id] as an identifier, it saves them by name. Note this is brittle if you save an object,
     * rename one of its fields, and then reload it.
     */
    class AllFields<R : Any>() : Serializer<R>(true) {

        protected var cachedFields = arrayOf<Field>()

        override fun onChangeType() {

            fun getFields(type: Class<*>): Array<Field> {
                var fields = type.declaredFields
                if (type.superclass != null) {
                    fields += getFields(type.superclass)
                }
                return fields
            }

            cachedFields = getFields(type)
            if (cachedFields.isNotEmpty()) {
                debugln("Found fields:")
                for (field in cachedFields) {
                    debugln("    ${field.name}: ${field.type.simpleName} ")
                }
            } else {
                debugln("Found no fields")
            }

            super.onChangeType()
        }

        override fun write(obj: Any, output: Output) {
            obj as R
            debugln("Writing number of fields: ${cachedFields.size}")
            output.writeInt(cachedFields.size)
            for (field in cachedFields) {
                var wasInaccessible = false
                if (!field.isAccessible) {
                    wasInaccessible = true
                    field.isAccessible = true
                }
                val fieldValue = field.get(obj)
                debugln("Writing field ${field.name}: ${field.type.simpleName} = ${fieldValue}")
                output.writeUTF(field.name)
                output.write(fieldValue)
                if (wasInaccessible) {
                    field.isAccessible = false
                }
            }
        }

        override fun read(newInstance: Any, input: Input) {
            newInstance as R
            val supposedSize = input.readInt()
            debugln("Supposed number of fields: $supposedSize")
            if (supposedSize < cachedFields.size) {
                throw ReadException("Instance in file does not have enough fields to cover all of class $type ($supposedSize in file vs ${cachedFields.size} needed)")
            } else if (supposedSize > cachedFields.size) {
                // if it is bigger, just with Serializer.Tagged, no cause for crashing, we just need to make sure that
                // this reads all the fields and doesn't leave the next one to be discovered by the next called of
                // input.read
            }
            // collect all name/value pairs
            val nameToValue = mutableMapOf<String, Any?>()
            for (i in 0 until supposedSize) {
                val fieldName = input.readUTF()
                val fieldValue = input.readUnknownNullable()
                nameToValue.put(fieldName, fieldValue)
            }
            val missingField = cachedFields.firstOrNull { it.name !in nameToValue.keys }
            if (missingField != null) {
                // uh oh
                throw ReadException("A field with name ${missingField.name} in $type was not in the file. File contains names: \n${nameToValue.keys.joinToString(separator = "\n")}")
            }
            for ((name, value) in nameToValue) {
                val field = cachedFields.firstOrNull { it.name == name }
                if (field == null) {
                    // the field existed in a previous version of the class but no longer does,
                    // that is ok because that just means it is no longer necessary
                    debugln("Found a field $name in the file that does not exist in the class $type")
                    val uselessValue = input.readUnknownNullable()
                    debugln("It had a value of $uselessValue")
                    continue
                }
                debugln("Reading field ${field.name}: ${field.type.simpleName} ")
                var wasInaccessible = false
                if (!field.isAccessible) {
                    wasInaccessible = true
                    field.isAccessible = true
                }
                // set the value of the tagged field by recursively deserializing it
                val nullable = field.kotlinProperty?.returnType?.isMarkedNullable
                if (nullable == false && value == null) {
                    throw ReadException("Read a null value on a non-nullable field $name")
                }
                field.set(newInstance, value)
                if (wasInaccessible) {
                    field.isAccessible = false
                }
            }
        }
    }
}

class EmptyListSerializer : Serializer<Collection<Nothing>>() {

    override fun write(obj: Any, output: Output) {
    }

    override fun instantiate(input: Input): Collection<Nothing> {
        return emptyList()
    }

    override fun read(newInstance: Any, input: Input) {
    }

}

/**
 * A serializer for any non-mutable [Collection]
 *
 * @param makeImmutable should take in a mutable collection and return an instance of [R] that is immutable
 */
class CollectionSerializer<R : Collection<*>>(val makeImmutable: (MutableCollection<*>) -> R) : Serializer<R>() {
    override fun instantiate(input: Input): R {
        val mutableCollection = mutableListOf<Any?>()
        val size = input.readUnsignedShort()
        for (i in 0 until size) {
            mutableCollection.add(input.readUnknownNullable())
        }
        return makeImmutable(mutableCollection)
    }

    override fun write(obj: Any, output: Output) {
        obj as Collection<Any?>
        output.writeShort(obj.size)
        for (element in obj) {
            output.write(element)
        }
    }

    override fun read(newInstance: Any, input: Input) {
    }
}

/**
 * A serializer for any mutable [Collection].
 *
 * Just works by default constructor initialization, and iterating through elements to serialize/deserialize them
 */
class MutableCollectionSerializer<R : MutableCollection<*>> : Serializer<R>(true) {

    override fun write(obj: Any, output: Output) {
        obj as MutableCollection<Any?>
        output.writeShort(obj.size)
        for (element in obj) {
            output.write(element)
        }
    }

    override fun read(newInstance: Any, input: Input) {
        newInstance as MutableCollection<Any?>
        val size = input.readUnsignedShort()
        for (i in 0 until size) {
            newInstance.add(input.readUnknownNullable())
        }
    }
}

/**
 * A serializer for mutable [Map]s. Works by converting the map to a list of [Pair]s and saving that as a mutable [Collection],
 * through [MutableCollectionSerializer]
 */
class MutableMapSerializer<R : MutableMap<*, *>> : Serializer<R>(true) {
    override fun write(obj: Any, output: Output) {
        obj as MutableMap<Any?, Any?>
        output.write(obj.entries.map { Pair(it.key, it.value) })
    }

    override fun read(newInstance: Any, input: Input) {
        newInstance as MutableMap<Any?, Any?>
        val entries = input.read(ArrayList::class.java) as ArrayList<Pair<Any?, Any?>>
        newInstance.putAll(entries)
    }
}

/**
 * A serializer for [Array]s of any type.
 */
class ArraySerializer : Serializer<Array<out Any?>>() {

    override fun write(obj: Any, output: Output) {
        obj as Array<*>
        output.writeInt(obj.size)
        output.writeInt(Registration.getId(obj::class.java.componentType))
        var index = 0
        while(index <= obj.lastIndex) {
            val element = obj[index]
            if(element == null) {
                // simplify writing lots of nulls
                var nullIndicies = 1
                while(index + nullIndicies <= obj.lastIndex) {
                    if(obj[index + nullIndicies] == null) {
                        nullIndicies++
                    } else {
                        break
                    }
                }
                debugln("$nullIndicies nulls in a row")
                output.writeShort(Primitive.NULL.id)
                output.writeInt(nullIndicies)
                index += nullIndicies
            } else {
                output.write(element)
                index++
            }
        }
    }

    override fun instantiate(input: Input): Array<Any?> {
        val size = input.readInt()
        val type = Registration.getType(input.readInt())
        val array = java.lang.reflect.Array.newInstance(type, size) as Array<Any?>
        var i = 0
        while(i < size) {
            val element = input.readUnknownNullable()
            if(element == null) {
                val nullCount = input.readInt()
                debugln("$nullCount null elements in a row")
                for(nullIndex in i until (i + nullCount)) {
                    array[nullIndex] = null
                }
                i += nullCount
            } else {
                array[i] = element
                i++
            }
        }
        return array
    }

    override fun read(newInstance: Any, input: Input) {
    }
}

/**
 * A serializer used for classes like [resource.ResourceType] or [graphics.Animation] which are defined statically, once, with values set in
 * code as essentially constants. The class must have one central collection, obtained from the [getAllPossibleValues] function,
 * against which to check its id obtained from the [getId] function.
 *
 * For example, the registration of the [ItemType] class includes the following instance of [IDSerializer]:
 *
 *
 *     IDSerializer({ItemType.ALL}, {it.id})
 *
 * What this does is essentially serializes each [ItemType] as the result of the [getId] function, in this case, `it.id`,
 * and when it reads it, it simply returns the first [ItemType] in [ItemType.ALL] with the given `id`. The `id` can be
 * any arbitrary value, but it is probably best left as some primitive or a [String]
 */
open class IDSerializer<R : Any>(val getAllPossibleValues: (type: Class<*>) -> List<R>, val getId: (R) -> Any) : Serializer<R>() {
    var values = listOf<Any>()

    override fun onChangeType() {
        values = getAllPossibleValues(type)
    }

    override fun write(obj: Any, output: Output) {
        obj as R
        output.write(getId(obj))
    }

    override fun instantiate(input: Input): R {
        val id = input.readUnknown()
        return values.firstOrNull { getId(it as R) == id } as R?
                ?: throw ReadException("Encountered a $type with id $id in the file, but none exists in $values")
    }

    override fun read(newInstance: Any, input: Input) {
    }
}

/**
 * This is an [IDSerializer] with parameter [IDSerializer.getAllPossibleValues] equal to `{ Enum.values() }` and parameter
 * [IDSerializer.getId] equal to `{ Enum.ordinal }`. See documentation of [Enum] and [IDSerializer] for more details
 */
class EnumSerializer<R : Enum<*>> : IDSerializer<R>({ type -> type.enumConstants.toList() as List<R> }, { it.ordinal }) {
    override fun onChangeType() {
        if (!type.isEnum) {
            throw RegistrationException("EnumSerializer cannot be used to serialize anything except Enums")
        }
        super.onChangeType()
    }
}

class PairSerializer : Serializer<Pair<Any?, Any?>>() {
    override fun write(obj: Any, output: Output) {
        obj as Pair<Any?, Any?>
        output.write(obj.first)
        output.write(obj.second)
    }

    override fun instantiate(input: Input): Pair<Any?, Any?> {
        return Pair(input.readUnknownNullable(), input.readUnknownNullable())
    }
}

class LambdaSerializer : Serializer<Any>() {
    override fun write(obj: Any, output: Output) {
        val javaOutput = ObjectOutputStream(output)
        javaOutput.writeObject(obj)
    }

    override fun instantiate(input: Input): Any {
        val javaInput = ObjectInputStream(input)
        val lambda = javaInput.readObject()
        return lambda
    }
}