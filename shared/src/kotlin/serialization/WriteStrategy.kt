package serialization

/**
 * Classes extending [CreateStrategy] must implement a constructor with one argument: [type]
 */
abstract class WriteStrategy<in T : Any>(val type: Class<*>, val settings: Set<SerializerSetting<*>>) {
    abstract fun write(obj: T, output: Output)

    class None(type: Class<*>, settings: Set<SerializerSetting<*>>) : WriteStrategy<Any>(type, settings) {
        override fun write(obj: Any, output: Output) {
        }
    }
}