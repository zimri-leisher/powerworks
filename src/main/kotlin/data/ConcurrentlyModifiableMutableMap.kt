package data

class ConcurrentlyModifiableMutableMap<T, K> {

    var beingTraversed = false

    val elements = mutableMapOf<T, K>()

    val toAdd = mutableMapOf<T, K>()

    val toRemove = mutableListOf<T>()

    fun put(l: T, o: K) {
        if (beingTraversed)
            toAdd.put(l, o)
        else
            elements.put(l, o)
    }

    fun remove(l: T) {
        if (beingTraversed)
            toRemove.add(l)
        else
            elements.remove(l)
    }

    val size
    get() = elements.size + toAdd.size - toRemove.size

    fun forEach(f: (T, K) -> Unit) {
        beingTraversed = true
        elements.forEach(f)
        beingTraversed = false
        elements.putAll(toAdd)
        toAdd.clear()
        toRemove.forEach { t -> elements.remove(t) }
        toRemove.clear()
    }

    operator fun iterator() = elements.iterator()

    fun clear() {
        elements.clear()
        toAdd.clear()
        toRemove.clear()
    }
}