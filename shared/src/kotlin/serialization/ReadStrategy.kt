package serialization

/**
 * Classes extending [CreateStrategy] must implement a constructor with one argument: [type]
 */
abstract class ReadStrategy<in T : Any>(val type: Class<*>, val settings: List<SerializerSetting<*>>) {
    abstract fun read(obj: T, input: Input)

    object None : ReadStrategy<Any>(Any::class.java, listOf()) {
        override fun read(obj: Any, input: Input) {
        }
    }
}