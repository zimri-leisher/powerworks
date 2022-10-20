package serialization

/**
 * Classes extending [CreateStrategy] must implement a constructor with one argument: [type]
 */
abstract class WriteStrategy<in T : Any>(val type: Class<*>, val settings: List<SerializerSetting<*>>) {
    abstract fun write(obj: T, output: Output)

    object None : WriteStrategy<Any>(Any::class.java, listOf()) {
        override fun write(obj: Any, output: Output) {
        }
    }
}