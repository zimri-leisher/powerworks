package serialization

abstract class Reference<out T : Any> {
    private var _value: Any? = null
    val value: T?
        get() = _value as T?

    fun setValue(value: Any?) {
        _value = value
    }

    abstract fun resolve(): T?
}

interface Referencable<out T : Any> {
    fun toReference(): Reference<T>
}

class UnresolvedReferenceException(val reference: Reference<*>) : java.lang.Exception()

class ReferencableCreateStrategy(type: Class<out Referencable<Any>>, settings: Set<SerializerSetting<*>>) :
    CreateStrategy<Referencable<Any>>(type, settings) {
    override fun create(input: Input): Referencable<Any> {
        val reference = input.read(Reference::class.java) as Reference<Any>
        reference.setValue(reference.resolve())
        if (reference.value == null) {
            // unable to resolve reference. what if we could pause resolution of references until we needed them?
            // we'd have to set up some system to store the reference class itself, because it basically gets discarded
            // after it is read here...
            // would have to associate the reference with the specific field
            // maybe we'd have a Serialization.resolveReferences()
            // it would check a hashmap of objects for which reference resolution failed
            // the map would have a list of failed references and fields to put them in
            // for each
            throw UnresolvedReferenceException(reference)
        }
        return reference.value as Referencable<Any>
    }
}

class ReferencableWriteStrategy(type: Class<out Referencable<Any>>, settings: Set<SerializerSetting<*>>) :
    WriteStrategy<Referencable<Any>>(type, settings) {
    override fun write(obj: Referencable<Any>, output: Output) {
        val reference = obj.toReference()
        output.write(reference)
    }
}