package data

open class ConcurrentlyModifiableMutableList<T> {

    var beingTraversed = false

    val elements = mutableListOf<T>()

    val toAdd = mutableListOf<T>()

    val toRemove = mutableListOf<T>()

    open fun add(l: T) {
        if (beingTraversed)
            toAdd.add(l)
        else
            elements.add(l)
    }

    open fun remove(l: T) {
        if (beingTraversed)
            toRemove.add(l)
        else
            elements.remove(l)
    }

    val size
        get() = elements.size + toAdd.size - toRemove.size

    fun forEach(f: (T) -> Unit) {
        beingTraversed = true
        elements.forEach(f)
        beingTraversed = false
        elements.addAll(toAdd)
        toAdd.clear()
        elements.removeAll(toRemove)
        toRemove.clear()
    }
}