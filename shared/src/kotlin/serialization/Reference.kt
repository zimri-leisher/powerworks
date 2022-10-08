package serialization

abstract class Reference<T> {
    var value: T? = null

    abstract fun resolve(): T?

    override fun toString() = value.toString()
}

interface Referencable<T> {
    fun toReference(): Reference<T>
}

class ReferencableCreateStrategy(type: Class<out Referencable<Any>>) :
    CreateStrategy<Referencable<Any>>(type) {
    override fun create(input: Input): Referencable<Any> {
        val reference = input.read(Reference::class.java) as Reference<Any>
        reference.value = reference.resolve()
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