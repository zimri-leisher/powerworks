package serialization

abstract class Reference<T : Any> {
    var value: T? = null

    abstract fun resolve(): T?

    override fun toString() = "${this::class.java.simpleName}($value)"
}

interface Referencable<T : Any> {
    fun toReference(): Reference<T>
}

class ReferencableCreateStrategy(type: Class<out Referencable<Any>>, settings: List<SerializerSetting<*>>) :
    CreateStrategy<Referencable<Any>>(type, settings) {
    override fun create(input: Input): Referencable<Any> {
        val reference = input.read(Reference::class.java) as Reference<Any>
        reference.value = reference.resolve()
        if (reference.value == null) {
            throw Exception("Reference $reference resolved to null")
        }
        return reference.value as Referencable<Any>
    }
}

class ReferencableWriteStrategy(type: Class<out Referencable<Any>>, settings: List<SerializerSetting<*>>) :
    WriteStrategy<Referencable<Any>>(type, settings) {
    override fun write(obj: Referencable<Any>, output: Output) {
        val reference = obj.toReference()
        output.write(reference)
    }
}

class RecursiveReferencableWriteStrategy(type: Class<out Any>, settings: List<SerializerSetting<*>>) :
    WriteStrategy<Any>(type, settings) {

    private val recursiveSettings = settings.filter { it !is RecursiveReferenceSetting }

    override fun write(obj: Any, output: Output) {
        // we want to write the object normally except if it is referencable
        if (Referencable::class.java.isAssignableFrom(obj::class.java)) {
            // write the reference
            // this should end the recursion
            output.write(obj, recursiveSettings + ReferenceSetting(AsReference()))
        } else {
            // call output.write with internalrecursive setting
            // this will call the normal serializer, but will include the @asreferencerecursive setting, so its own calls
            // to serializers will call this first
            output.write(obj, recursiveSettings + InternalRecurseSetting(AsReferenceRecursive()))
        }
    }
}

class RecursiveReferencableCreateStrategy(type: Class<out Any>, settings: List<SerializerSetting<*>>) :
    CreateStrategy<Any>(type, settings) {

    private val recursiveSettings = settings.filter { it !is RecursiveReferenceSetting }

    override fun create(input: Input): Any {
        if (Referencable::class.java.isAssignableFrom(type)) {
            return input.read(type, recursiveSettings + ReferenceSetting(AsReference()))
        } else {
            return input.read(type, recursiveSettings + InternalRecurseSetting(AsReferenceRecursive()))
        }
    }
}