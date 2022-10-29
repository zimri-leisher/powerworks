package serialization

/**
 * Classes extending [CreateStrategy] must implement a constructor with one argument: [type]
 */
abstract class ReadStrategy<in T : Any>(val type: Class<*>, val settings: Set<SerializerSetting<*>>) {
    abstract fun read(obj: T, input: Input)

    class None(type: Class<*>, settings: Set<SerializerSetting<*>>) : ReadStrategy<Any>(type, settings) {
        override fun read(obj: Any, input: Input) {
        }
    }
}