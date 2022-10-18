package serialization

abstract class Reference<T : Any> {
    var value: T? = null

    abstract fun resolve(): T?

    override fun toString() = value.toString()
}

interface Referencable<T : Any> {
    fun toReference(): Reference<T>
}

class ReferencableCreateStrategy(type: Class<out Referencable<Any>>) :
    CreateStrategy<Referencable<Any>>(type) {
    override fun create(input: Input): Referencable<Any> {
        val reference = input.read(Reference::class.java) as Reference<Any>
        reference.value = reference.resolve()
        if (reference.value == null) {
            throw Exception("Reference $reference resolved to null")
        }
        return reference.value as Referencable<Any>
    }
}

class ReferencableWriteStrategy(type: Class<out Referencable<Any>>) :
    WriteStrategy<Referencable<Any>>(type) {
    override fun write(obj: Referencable<Any>, output: Output) {
        val reference = obj.toReference()
        output.write(reference)
    }
}

class RecursiveReferencableWriteStrategy(type: Class<out Any>) : WriteStrategy<Any>(type) {
    override fun write(obj: Any, output: Output) {
        // we want to write the object normally except if it is referencable
        if(Referencable::class.java.isAssignableFrom(obj::class.java)) {

        }
    }
}

// want to be able to serialize reference arrays, maps, lists
class ReferencableIteratorWriteStrategy(type: Class<out Iterable<Referencable<Any>>>) :
    WriteStrategy<Iterable<Referencable<Any>>>(type) {
    override fun write(obj: Iterable<Referencable<Any>>, output: Output) {
        output.write(obj)
    }
}