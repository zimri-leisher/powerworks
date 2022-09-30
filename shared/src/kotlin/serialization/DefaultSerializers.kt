package serialization

import item.ItemType
import main.isKotlinClass
import java.lang.reflect.Field
import kotlin.reflect.KProperty1
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties

class EmptyListSerializer(type: Class<*>, settings: List<SerializerSetting<*>>) :
    Serializer<Collection<Nothing>>(type, settings) {
    override val createStrategy = object : CreateStrategy<Collection<Nothing>>(type) {
        override fun create(input: Input): Collection<Nothing> {
            return emptyList()
        }
    }
}

/**
 * A serializer for any non-mutable [Collection]
 *
 * @param makeImmutable should take in a mutable collection and return an instance of [R] that is immutable
 */
class CollectionSerializer<R : Collection<*>>(
    type: Class<R>,
    settings: List<SerializerSetting<*>>,
    val makeImmutable: (MutableCollection<*>) -> R
) : Serializer<R>(type, settings) {
    override val createStrategy = object : CreateStrategy<R>(type) {
        override fun create(input: Input): R {
            val mutableCollection = mutableListOf<Any?>()
            val size = input.readUnsignedShort()
            for (i in 0 until size) {
                mutableCollection.add(input.readUnknownNullable())
            }
            return makeImmutable(mutableCollection)
        }
    }

    override val writeStrategy = object : WriteStrategy<R>(type) {
        override fun write(obj: R, output: Output) {
            output.writeShort(obj.size)
            for (element in obj) {
                output.write(element)
            }
        }
    }
}

/**
 * A serializer for any mutable [Collection].
 *
 * Just works by default constructor initialization, and iterating through elements to serialize/deserialize them
 */
class MutableCollectionSerializer<R : MutableCollection<*>>(type: Class<R>, settings: List<SerializerSetting<*>>) :
    Serializer<R>(type, settings) {

    override val createStrategy = EmptyConstructorCreateStrategy(type)

    override val writeStrategy = object : WriteStrategy<R>(type) {
        override fun write(obj: R, output: Output) {
            obj as MutableCollection<Any?>
            output.writeShort(obj.size)
            for (element in obj) {
                output.write(element)
            }
        }
    }

    override val readStrategy = object : ReadStrategy<R>(type) {
        override fun read(obj: R, input: Input) {
            obj as MutableCollection<Any?>
            val size = input.readUnsignedShort()
            for (i in 0 until size) {
                obj.add(input.readUnknownNullable())
            }
        }
    }
}

class EmptyMapSerializer(type: Class<Map<Nothing, Nothing>>, settings: List<SerializerSetting<*>>) :
    Serializer<Map<Nothing, Nothing>>(type, settings) {

    override val createStrategy = object : CreateStrategy<Map<Nothing, Nothing>>(type) {
        override fun create(input: Input): Map<Nothing, Nothing> {
            return emptyMap()
        }
    }
}


/**
 * A serializer for any non-mutable [Collection]
 *
 * @param makeImmutable should take in a mutable collection and return an instance of [R] that is immutable
 */
class MapSerializer<R : Map<*, *>>(
    type: Class<R>,
    settings: List<SerializerSetting<*>>,
    val makeImmutable: (MutableMap<*, *>) -> R
) : Serializer<R>(type, settings) {

    override val createStrategy = object : CreateStrategy<R>(type) {
        override fun create(input: Input): R {
            val mutableCollection = mutableMapOf<Any?, Any?>()
            val size = input.readUnsignedShort()
            for (i in 0 until size) {
                val pair = input.read(Pair::class.java)
                mutableCollection[pair.first] = pair.second
            }
            return makeImmutable(mutableCollection)
        }
    }

    override val writeStrategy = object : WriteStrategy<R>(type) {
        override fun write(obj: R, output: Output) {
            output.writeShort(obj.size)
            for ((first, second) in obj) {
                output.write(first to second)
            }
        }
    }
}

/**
 * A serializer for mutable [Map]s. Works by converting the map to a list of [Pair]s and saving that as a mutable [Collection],
 * through [MutableCollectionSerializer]
 */
class MutableMapSerializer<R : MutableMap<*, *>>(type: Class<R>, settings: List<SerializerSetting<*>>) :
    Serializer<R>(type, settings) {

    override val createStrategy = EmptyConstructorCreateStrategy(type)

    override val writeStrategy = object : WriteStrategy<R>(type) {
        override fun write(obj: R, output: Output) {
            output.write(obj.entries.map { Pair(it.key, it.value) })
        }
    }

    override val readStrategy = object : ReadStrategy<R>(type) {
        override fun read(obj: R, input: Input) {
            obj as MutableMap<Any?, Any?>
            val entries = input.read(ArrayList::class.java) as ArrayList<Pair<Any?, Any?>>
            obj.putAll(entries)
        }
    }
}

/**
 * A serializer for [Array]s of any type.
 */
class ArraySerializer(type: Class<Array<out Any?>>, settings: List<SerializerSetting<*>>) :
    Serializer<Array<out Any?>>(type, settings) {

    override val createStrategy = object : CreateStrategy<Array<out Any?>>(type) {
        override fun create(input: Input): Array<out Any?> {
            val size = input.readInt()
            val arrayType = Registration.getType(input.readInt())
            val array = java.lang.reflect.Array.newInstance(arrayType, size) as Array<Any?>
            var i = 0
            while (i < size) {
                val element = input.readUnknownNullable()
                if (element == null) {
                    val nullCount = input.readInt()
                    SerializerDebugger.writeln("$nullCount null elements in a row")
                    for (nullIndex in i until (i + nullCount)) {
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
    }

    override val writeStrategy = object : WriteStrategy<Array<out Any?>>(type) {
        override fun write(obj: Array<out Any?>, output: Output) {
            // todo configure between sparse and not
            output.writeInt(obj.size)
            output.writeInt(Registration.getId(obj::class.java.componentType))
            var index = 0
            while (index <= obj.lastIndex) {
                val element = obj[index]
                if (element == null) {
                    // simplify writing lots of nulls
                    var nullIndicies = 1
                    while (index + nullIndicies <= obj.lastIndex) {
                        if (obj[index + nullIndicies] == null) {
                            nullIndicies++
                        } else {
                            break
                        }
                    }
                    SerializerDebugger.writeln("$nullIndicies nulls in a row")
                    output.writeShort(Primitive.NULL.id)
                    output.writeInt(nullIndicies)
                    index += nullIndicies
                } else {
                    output.write(element)
                    index++
                }
            }
        }
    }
}

open class IDSerializer<R : Any>(
    type: Class<R>,
    settings: List<SerializerSetting<*>>,
    val getAllPossibleValues: (type: Class<*>) -> List<R>,
    open val getId: (R) -> Any
) : Serializer<R>(type, settings) {

    val values = getAllPossibleValues(type)

    override val createStrategy = object : CreateStrategy<R>(type) {
        override fun create(input: Input): R {
            val id = input.readUnknown()
            return values.firstOrNull { getId(it) == id }
                ?: throw ReadException("IDSerializer encountered a $type with id $id in the file, but none exists in $values")

        }
    }
}

/**
 * A serializer used for classes like [resource.ResourceType] or [graphics.Animation] which are defined statically, once, with values set in
 * code as essentially constants. The class must have one central collection, obtained from the [getAllPossibleValues] function,
 * against which to check its id obtained from the [getId] function.
 *
 * For example, the registration of the [ItemType] class includes the following instance of [AutoIDSerializer]:
 *
 *
 *     IDSerializer({ItemType.ALL}, {it.id})
 *
 * What this does is essentially serializes each [ItemType] as the result of the [getId] function, in this case, `it.id`,
 * and when it reads it, it simply returns the first [ItemType] in [ItemType.ALL] with the given `id`. The `id` can be
 * any arbitrary value, but it is probably best left as some primitive or a [String]
 */
open class AutoIDSerializer<R : Any>(
    type: Class<R>,
    settings: List<SerializerSetting<*>>
) :
    IDSerializer<R>(type, settings, { getAllPossibleValues(it) as List<R> }, {}) {

    val idGetter = getIdGetter(type)

    override val getId: (R) -> Any
        get() = {
            idGetter.get(it)
        }

    companion object {
        private fun getIdGetter(type: Class<*>): Field {
            for (field in type.fields) {
                if (field.isAnnotationPresent(ObjectIdentifier::class.java)) {
                    return field
                }
            }
            throw Exception("No fields in class $type marked @ObjectIdentifier")
        }

        private fun getAllPossibleValues(type: Class<*>): List<*> {
            if (!type.isKotlinClass()) {
                throw Exception("Cannot use IDSerializer on non-Kotlin classes")
            }
            val companion =
                type.kotlin.companionObject ?: throw Exception("IDSerializable classes must have companion objects")
            val fields = companion.memberProperties.filter { it.hasAnnotation<ObjectList>() }
            if (fields.size != 1) {
                throw Exception("More than 1 field with the @ObjectList annotation: ${fields.joinToString()}")
            }
            val objectList = fields[0] as KProperty1<Any?, Any?>
            if (!List::class.java.isAssignableFrom(objectList.returnType.javaClass)) {
                throw Exception("@ObjectList field does not extend List")
            }
            val objects = objectList.get(type.kotlin.companionObjectInstance) as List<R>
            for (obj in objects) {
                if (!type.isAssignableFrom(obj::class.java)) {
                    throw Exception("@ObjectList does not only contain $type")
                }
            }
            return objects
        }
    }
}

/**
 * This is an [AutoIDSerializer] with parameter [AutoIDSerializer.getAllPossibleValues] equal to `{ Enum.values() }` and parameter
 * [AutoIDSerializer.getId] equal to `{ Enum.ordinal }`. See documentation of [Enum] and [AutoIDSerializer] for more details
 */
class EnumSerializer<R : Enum<*>>(type: Class<R>, settings: List<SerializerSetting<*>>) :
    IDSerializer<R>(type, settings, { type.enumConstants.toList() }, { it.ordinal }) {

    init {
        if (!type.isEnum) {
            throw RegistrationException("EnumSerializer cannot be used to serialize anything except Enums")
        }
    }
}

class PairSerializer(type: Class<Pair<Any?, Any?>>, settings: List<SerializerSetting<*>>) :
    Serializer<Pair<Any?, Any?>>(type, settings) {

    override val createStrategy = object : CreateStrategy<Pair<Any?, Any?>>(type) {
        override fun create(input: Input): Pair<Any?, Any?> {
            return Pair(input.readUnknownNullable(), input.readUnknownNullable())
        }
    }

    override val writeStrategy = object : WriteStrategy<Pair<Any?, Any?>>(type) {
        override fun write(obj: Pair<Any?, Any?>, output: Output) {
            output.write(obj.first)
            output.write(obj.second)
        }
    }
}