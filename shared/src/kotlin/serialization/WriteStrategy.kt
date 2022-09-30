package serialization

/**
 * Classes extending [CreateStrategy] must implement a constructor with one argument: [type]
 */
abstract class WriteStrategy<in T : Any>(val type: Class<*>) {
    abstract fun write(obj: T, output: Output)

    object None : WriteStrategy<Any>(Any::class.java) {
        override fun write(obj: Any, output: Output) {
        }
    }
}