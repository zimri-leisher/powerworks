package main

import java.lang.IllegalStateException
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

object PowerworksDelegates {
    /**
     * A (non-null) value initialized to null that can only be set once
     */
    fun <T : Any> lateinitVal() = LateinitVal<T>()
}

class LateinitVal<T : Any> : ReadWriteProperty<Any?, T> {

    private var value: T? = null
    var initialized = false

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value!!
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        if(initialized) {
            throw IllegalStateException("Cannot set the value of a LateinitVal twice (tried to set ${property.name} to $value when it was already ${this.value})")
        }
        this.value = value
        initialized = true
    }
}