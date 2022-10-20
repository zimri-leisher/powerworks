package serialization

import java.lang.reflect.Constructor
import java.lang.reflect.Modifier

/**
 * Classes extending [CreateStrategy] must implement a constructor with one argument: [type]
 */
abstract class CreateStrategy<out T : Any>(val type: Class<*>, val settings: List<SerializerSetting<*>>) {
    object None : CreateStrategy<Any>(Any::class.java, listOf()) {
        override fun create(input: Input): Any {
            return Any()
        }
    }

    abstract fun create(input: Input): T
}

class EmptyConstructorCreateStrategy<T : Any>(type: Class<T>, settings: List<SerializerSetting<*>>) :
    CreateStrategy<T>(type, settings) {

    private lateinit var cachedConstructor: Constructor<T>

    init {
        if (type.isInterface || Modifier.isAbstract(type.modifiers)) {
            // default won't happen with either of these because for any instance of these the actual type will be something else
            // do nothing
        } else {
            val defaultConstructor = type.declaredConstructors.firstOrNull { it.parameterCount == 0 }
            if (defaultConstructor == null) {
                SerializerDebugger.writeln("(No default (0-arg) constructor, checking if $type is member class)")
                if (type.isMemberClass) {
                    SerializerDebugger.writeln("($type is member class)")
                    // needs an instance of superclass to instantiate
                    cachedConstructor =
                        type.declaredConstructors.firstOrNull { it.parameterCount == 1 } as Constructor<T>?
                            ?: throw ReadException("Member class $type has no default constructor")
                    if (!cachedConstructor.isAccessible) {
                        cachedConstructor.isAccessible = true
                    }
                } else {
                    throw InstantiationException("Class $type has no default constructor")
                }
            } else {
                if (!defaultConstructor.isAccessible) {
                    defaultConstructor.isAccessible = true
                }
                cachedConstructor = defaultConstructor as Constructor<T>
            }
        }

    }

    /**
     * Given an input stream [input], this function should return an instance of class [type]. If a class needs to be serialized
     * but cannot have a default (0 argument) constructor, one should override this method and use the [input] stream to
     * read in appropriate arguments for its constructor.
     *
     * If you wish to use default constructor initialization but still want to override this method, make sure to call
     * `super.instantiate(input)` inside of the override.
     *
     * In the case that you are overriding this method to simply read some single value from [input] and return an already
     * existing instance of class [type] that is stored in a list somewhere, you can use the [AutoIDSerializer] instead, which makes
     * that significantly more easy. For more details see the documentation for [AutoIDSerializer]
     */
    override fun create(input: Input): T {
        SerializerDebugger.writeln("(Using default (0-arg) constructor instantiation for $type)")
        if (cachedConstructor.parameterCount == 1) {
            val superClass = type.enclosingClass
            val superClassInstance = input.instantiate(superClass)
            return cachedConstructor.newInstance(superClassInstance) as T
        }
        return cachedConstructor.newInstance() as T
    }
}