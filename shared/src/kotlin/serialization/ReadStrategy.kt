package serialization

/**
 * Classes extending [CreateStrategy] must implement a constructor with one argument: [type]
 */
abstract class ReadStrategy<in T : Any>(val type: Class<*>) {
    abstract fun read(obj: T, input: Input)

    object None : ReadStrategy<Any>(Any::class.java) {
        override fun read(obj: Any, input: Input) {
        }
    }
}