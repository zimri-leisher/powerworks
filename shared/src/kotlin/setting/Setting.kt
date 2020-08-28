package setting

import serialization.Id

sealed class Setting<T>(
        @Id(1)
        val name: String, val isValid: (T) -> Boolean) {

    abstract fun get(): T
    fun trySet(value: Any?) {
        try {
            value as T
            if (isValid(value)) {
                set(value)
                return
            }
        } catch (e: ClassCastException) { }
        println("Illegal value for setting $name: $value")
    }

    abstract fun set(value: T): Setting<T>
}

class UnknownSetting : Setting<Unit>("Unknown Setting", { false }) {
    override fun get() {
    }

    override fun set(value: Unit) = this
}

class IntSetting(name: String, isValid: (Int) -> Boolean) : Setting<Int>(name, isValid) {
    private constructor() : this("", {false})

    @Id(2)
    var value = 0

    override fun get() = value

    override fun set(value: Int): Setting<Int> {
        this.value = value
        return this
    }
}