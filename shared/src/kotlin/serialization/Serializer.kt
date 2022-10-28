package serialization

import java.lang.reflect.Constructor


// we want the serializer to be set by default
// but then we want it to change based on annotations
// is there anything else that could cause the serializer to change?
// what context determines which serializer we use?
// the type, whether we want it as a reference, maybe compression type? alright, clearly we should
// make annotations able to specify arbitrary strategies Ooh, this would be just what I need for sending levels vs level references

// now that we have a better understanding of the system...
// there are default strategies for how to serialize
// read/write strats come in pairs
//  always? well sometimes create/write comes in pairs
// we definitely want them to have internal state, right? cuz without internal state we have to (for taggedserializer)
// iterate thru all fields every time, super bad :(
// but we also want to have a new serializer for each list of settings/type
// also, note that we never actually need a serializer instance for an abstract/interface type
// i guess we shouldn't even need to register abstract/interface types
// TODO remove registration of all abstract/interface classes
// 

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
open class Serializer<R : Any>(
    val type: Class<*>, val settings: List<SerializerSetting<*>>,
    open val createStrategy: CreateStrategy<R> = CreateStrategy.None as CreateStrategy<R>,
    /**
     * Writes the entire [obj] to the [output] stream. [obj] is guaranteed to be an instance of [type], so casting it to [R] is safe.
     * It's not defined as `obj: R` here because I am not good enough with generics.
     *
     * For all objects which [java.io.DataOutputStream] has a specific method for (e.g. `writeInt`, `writeUTF` which writes [String]s,
     * `writeFloat`, etc.), it is best practice to call these directly on [output] instead of calling the generic [Output.write] function,
     * which is slightly more performance heavy for those basic types.
     *
     */
    open val writeStrategy: WriteStrategy<R> = WriteStrategy.None,
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
    open val readStrategy: ReadStrategy<R> = ReadStrategy.None,
) {
    constructor(type: Class<*>, settings: List<SerializerSetting<*>>) : this(
        type,
        settings,
        CreateStrategy.None as CreateStrategy<R>,
        WriteStrategy.None,
        ReadStrategy.None
    )

    override fun toString(): String {
        return "${this::class.java.simpleName}(${type.simpleName}, [${settings.joinToString()}], ${createStrategy::class.java}, ${writeStrategy::class.java}, ${readStrategy::class.java})"
    }
}