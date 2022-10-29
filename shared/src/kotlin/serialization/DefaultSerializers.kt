package serialization

import item.ItemType
import main.isKotlinClass
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.*
import kotlin.reflect.KProperty1
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

class EmptyListSerializer(type: Class<*>, settings: List<SerializerSetting<*>>) :
    Serializer<Collection<Nothing>>(type, settings) {
    override val createStrategy = object : CreateStrategy<Collection<Nothing>>(type, settings) {
        override fun create(input: Input): Collection<Nothing> {
            return emptyList()
        }
    }
}

class CollectionCreateStrategy<R : Collection<*>>(type: Class<R>, settings: List<SerializerSetting<*>>) :
    CreateStrategy<R>(type, settings) {
    override fun create(input: Input): R {
        val mutableCollection = mutableListOf<Any?>()
        val size = input.readUnsignedShort()
        for (i in 0 until size) {
            mutableCollection.add(input.readUnknownNullable(settings))
        }
        if (Set::class.java.isAssignableFrom(type)) {
            return mutableCollection.toSet() as R
        } else {
            return mutableCollection.toList() as R
        }
    }
}

class CollectionWriteStrategy<R : Collection<*>>(type: Class<R>, settings: List<SerializerSetting<*>>) :
    WriteStrategy<R>(type, settings) {
    override fun write(obj: R, output: Output) {
        output.writeShort(obj.size)
        for (element in obj) {
            output.write(element, settings)
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
    settings: List<SerializerSetting<*>>
) : Serializer<R>(type, settings) {
    override val createStrategy = CollectionCreateStrategy(type, settings)

    override val writeStrategy = CollectionWriteStrategy(type, settings)
}

class MutableCollectionWriteStrategy<R : MutableCollection<*>>(type: Class<R>, settings: List<SerializerSetting<*>>) :
    WriteStrategy<R>(type, settings) {
    override fun write(obj: R, output: Output) {
        obj as MutableCollection<Any?>
        output.writeShort(obj.size)
        for (element in obj) {
            output.write(element, settings)
        }
    }
}

class MutableCollectionReadStrategy<R : MutableCollection<*>>(type: Class<R>, settings: List<SerializerSetting<*>>) :
    ReadStrategy<R>(type, settings) {
    override fun read(obj: R, input: Input) {
        obj as MutableCollection<Any?>
        val size = input.readUnsignedShort()
        for (i in 0 until size) {
            obj.add(input.readUnknownNullable(settings))
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

    override val createStrategy = EmptyConstructorCreateStrategy(type, settings)

    override val writeStrategy = MutableCollectionWriteStrategy(type, settings)

    override val readStrategy = MutableCollectionReadStrategy(type, settings)
}

class EmptyMapCreateStrategy(type: Class<Map<Nothing, Nothing>>, settings: List<SerializerSetting<*>>) :
    CreateStrategy<Map<Nothing, Nothing>>(type, settings) {
    override fun create(input: Input): Map<Nothing, Nothing> {
        return emptyMap()
    }
}

class EmptyMapSerializer(type: Class<Map<Nothing, Nothing>>, settings: List<SerializerSetting<*>>) :
    Serializer<Map<Nothing, Nothing>>(type, settings) {

    override val createStrategy = EmptyMapCreateStrategy(type, settings)
}

class MapCreateStrategy<R : Map<Any?, Any?>>(type: Class<R>, settings: List<SerializerSetting<*>>) :
    CreateStrategy<R>(type, settings) {
    override fun create(input: Input): R {
        val mutableCollection = mutableMapOf<Any?, Any?>()
        val size = input.readInt()
        for (i in 0 until size) {
            val pair = input.read(Pair::class.java, settings)
            mutableCollection[pair.first] = pair.second
        }
        return mutableCollection as R
    }
}

class MapWriteStrategy<R : Map<Any?, Any?>>(type: Class<R>, settings: List<SerializerSetting<*>>) :
    WriteStrategy<R>(type, settings) {
    override fun write(obj: R, output: Output) {
        output.writeInt(obj.size)
        for ((first, second) in obj) {
            output.write(first to second, settings)
        }
    }
}

/**
 * A serializer for any non-mutable [Collection]
 *
 * @param makeImmutable should take in a mutable collection and return an instance of [R] that is immutable
 */
class MapSerializer<R : Map<Any?, Any?>>(
    type: Class<R>,
    settings: List<SerializerSetting<*>>
) : Serializer<R>(type, settings) {

    override val createStrategy = MapCreateStrategy(type, settings)

    override val writeStrategy = MapWriteStrategy(type, settings)
}

class MutableMapWriteStrategy<R : MutableMap<*, *>>(type: Class<R>, settings: List<SerializerSetting<*>>) :
    WriteStrategy<R>(type, settings) {
    override fun write(obj: R, output: Output) {
        output.write(obj.entries.map { Pair(it.key, it.value) }, settings)
    }
}

class MutableMapReadStrategy<R : MutableMap<*, *>>(type: Class<R>, settings: List<SerializerSetting<*>>) :
    ReadStrategy<R>(type, settings) {
    override fun read(obj: R, input: Input) {
        obj as MutableMap<Any?, Any?>
        val entries = input.read(ArrayList::class.java, settings) as ArrayList<Pair<Any?, Any?>>
        obj.putAll(entries)
    }
}

/**
 * A serializer for mutable [Map]s. Works by converting the map to a list of [Pair]s and saving that as a mutable [Collection],
 * through [MutableCollectionSerializer]
 */
class MutableMapSerializer<R : MutableMap<*, *>>(type: Class<R>, settings: List<SerializerSetting<*>>) :
    Serializer<R>(type, settings) {

    override val createStrategy = EmptyConstructorCreateStrategy(type, settings)

    override val writeStrategy = MutableMapWriteStrategy(type, settings)

    override val readStrategy = MutableMapReadStrategy(type, settings)
}

class ArrayCreateStrategy(type: Class<Array<out Any?>>, settings: List<SerializerSetting<*>>) :
    CreateStrategy<Array<out Any?>>(type, settings) {
    override fun create(input: Input): Array<out Any?> {
        val size = input.readInt()
        val arrayType = Registration.getType(input.readInt())
        val array = java.lang.reflect.Array.newInstance(arrayType, size) as Array<Any?>
        var i = 0
        while (i < size) {
            val element = input.readUnknownNullable()
            if (element == null) {
                println("found settings $settings")
                if (settings.any { it is SparseSetting }) {
                    val nullCount = input.readInt()
                    SerializerDebugger.writeln("$nullCount null elements in a row")
                    for (nullIndex in i until (i + nullCount)) {
                        array[nullIndex] = null
                    }
                    i += nullCount
                } else {
                    array[i] = null
                    i++
                }
            } else {
                array[i] = element
                i++
            }
        }
        return array
    }
}

class ArrayWriteStrategy(type: Class<Array<out Any?>>, settings: List<SerializerSetting<*>>) :
    WriteStrategy<Array<out Any?>>(type, settings) {
    override fun write(obj: Array<out Any?>, output: Output) {
        output.writeInt(obj.size)
        output.writeInt(Registration.getId(obj::class.java.componentType))
        var index = 0
        while (index <= obj.lastIndex) {
            val element = obj[index]
            if (element == null) {
                if (settings.any { it is SparseSetting }) {
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
                    output.write(null, settings)
                    index++
                }
            } else {
                output.write(element, settings)
                index++
            }
        }
    }
}

/**
 * A serializer for [Array]s of any type.
 */
class ArraySerializer(type: Class<Array<out Any?>>, settings: List<SerializerSetting<*>>) :
    Serializer<Array<out Any?>>(type, settings) {

    override val createStrategy = ArrayCreateStrategy(type, settings)

    override val writeStrategy = ArrayWriteStrategy(type, settings)
}

// TODO how to handle this? can't split it up because there are fields in the serializer class

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
    settings: List<SerializerSetting<*>>,
) : Serializer<R>(type, settings) {

    val idGetter = getIdGetter(type)
    val values = getAllPossibleValues(type) as List<R>

    override val createStrategy = object : CreateStrategy<R>(type, settings) {
        override fun create(input: Input): R {
            val id = input.readUnknown(settings)
            return values.firstOrNull { idGetter(it) == id }
                ?: throw ReadException("IDSerializer encountered a $type with id $id in the file, but none exists in $values")

        }
    }

    override val writeStrategy = object : WriteStrategy<R>(type, settings) {
        override fun write(obj: R, output: Output) {
            val id = idGetter(obj)
            output.write(id, settings)
        }
    }

    companion object {
        private fun getIdGetter(type: Class<*>): (Any) -> Any {
            println(type.declaredMethods.joinToString())
            for (field in type.fields) {
                if (field.isAnnotationPresent(ObjectIdentifier::class.java)) {
                    return { field.get(it) }
                }
            }
            for (field in type.kotlin.members) {
                if (field.hasAnnotation<ObjectIdentifier>()) {
                    return { field.call(it)!! }
                }
            }
            for (method in type.methods) {
                if (method.isAnnotationPresent(ObjectIdentifier::class.java)) {
                    if (method.parameterCount != 0) {
                        throw Exception("Method $method was marked @ObjectIdentifier but its parameter count was not 0")
                    }
                    return { method.invoke(it) }
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
            val fields = companion.memberProperties.map { it.javaField!! }
                .filter { it.isAnnotationPresent(ObjectList::class.java) }

            if (fields.size > 1) {
                throw Exception("More than 1 field with the @ObjectList annotation: ${fields.joinToString()}")
            } else if (fields.isEmpty()) {
                throw Exception("No fields with @ObjectList annotation")
            }
            val objectList = fields[0]
            if (!List::class.java.isAssignableFrom(objectList.type)) {
                throw Exception("@ObjectList field does not extend List")
            }
            val wasAccessible = objectList.canAccess(null)
            objectList.isAccessible = true
            val objects = objectList.get(null) as List<*>
            for (obj in objects) {
                if (obj == null || !type.isAssignableFrom(obj::class.java)) {
                    throw Exception("@ObjectList does not only contain $type, it contained $obj")
                }
            }
            objectList.isAccessible = wasAccessible
            return objects
        }
    }
}


class EnumWriteStrategy<R : Enum<*>>(type: Class<R>, settings: List<SerializerSetting<*>>) :
    WriteStrategy<R>(type, settings) {
    override fun write(obj: R, output: Output) {
        output.write(obj.ordinal)
    }
}

class EnumCreateStrategy<R : Enum<*>>(type: Class<R>, settings: List<SerializerSetting<*>>) :
    CreateStrategy<R>(type, settings) {
    override fun create(input: Input): R {
        val id = input.readInt()
        return type.enumConstants.first { (it as R).ordinal == id } as R
    }
}

/**
 * This is an [AutoIDSerializer] with parameter [AutoIDSerializer.getAllPossibleValues] equal to `{ Enum.values() }` and parameter
 * [AutoIDSerializer.getId] equal to `{ Enum.ordinal }`. See documentation of [Enum] and [AutoIDSerializer] for more details
 */
class EnumSerializer<R : Enum<*>>(type: Class<R>, settings: List<SerializerSetting<*>>) :
    Serializer<R>(type, settings) {

    init {
        if (!type.isEnum) {
            throw RegistrationException("EnumSerializer cannot be used to serialize anything except Enums")
        }
    }

    override val createStrategy = EnumCreateStrategy(type, settings)

    override val writeStrategy = EnumWriteStrategy(type, settings)
}

class PairSerializer(type: Class<Pair<Any?, Any?>>, settings: List<SerializerSetting<*>>) :
    Serializer<Pair<Any?, Any?>>(type, settings) {

    override val createStrategy = object : CreateStrategy<Pair<Any?, Any?>>(type, settings) {
        override fun create(input: Input): Pair<Any?, Any?> {
            return Pair(input.readUnknownNullable(settings), input.readUnknownNullable(settings))
        }
    }

    override val writeStrategy = object : WriteStrategy<Pair<Any?, Any?>>(type, settings) {
        override fun write(obj: Pair<Any?, Any?>, output: Output) {
            output.write(obj.first, settings)
            output.write(obj.second, settings)
        }
    }
}

class UUIDSerializer(type: Class<UUID>, settings: List<SerializerSetting<*>>) : Serializer<UUID>(type, settings) {

    override val createStrategy = object : CreateStrategy<UUID>(type, settings) {
        override fun create(input: Input): UUID {
            return UUID.fromString(input.readUTF())
        }
    }

    override val writeStrategy = object : WriteStrategy<UUID>(type, settings) {
        override fun write(obj: UUID, output: Output) {
            output.writeUTF(obj.toString())
        }
    }
}