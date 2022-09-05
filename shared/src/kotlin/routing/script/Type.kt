package routing.script

import resource.ResourceType

sealed class Type<R>(parent: Type<in R>? = null) {

    private val _parent = parent

    val parent: Type<in R>
        get() = _parent ?: Any
    // if parent is null, just use None as parent


    object Any : Type<kotlin.Any?>()
    object None : Type<Unit>()
    object Number : Type<kotlin.Number>()
    object Int : Type<kotlin.Int>(Number)
    object Double : Type<kotlin.Double>(Number)
    object ResourceType : Type<resource.ResourceType>()
    object Boolean : Type<kotlin.Boolean>()
    object List : Type<kotlin.collections.List<Any?>>()

    override fun toString(): String {
        return this::class.simpleName!!
    }
}