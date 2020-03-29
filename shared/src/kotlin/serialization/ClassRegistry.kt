package serialization

import java.io.DataInputStream
import java.io.DataOutputStream

class ClassRegistry<R : Any>(val type: Class<R>, serializer: Serializer<*>) {

    var serializer = serializer
        private set

    init {
        serializer.type = type
    }

    fun setSerializer(serializer: Serializer<R>) = this.apply { this.serializer = serializer; serializer.type = this.type }
    fun setSerializer(instantiator: (input: Input) -> R, reader: (newInstance: R, input: Input) -> Unit, writer: (obj: R, output: Output) -> Unit) = this.apply {
        serializer = object : Serializer<R>() {

            override fun instantiate(input: Input): R {
                return instantiator(input)
            }

            override fun read(newInstance: Any, input: Input) {
                reader(newInstance as R, input)
            }

            override fun write(obj: Any, output: Output) {
                writer(obj as R, output)
            }
        }
        serializer.type = type
    }
}