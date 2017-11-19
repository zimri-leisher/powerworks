package misc

class ConcurrentlyModifiableMutableMap<T, K> {

    var beingTraversed = false

    val elements = mutableMapOf<T, K>()

    val toAdd = mutableMapOf<T, K>()

    val toRemove = mutableMapOf<T, K>()

    fun put(l: T, o: K) {
        if (beingTraversed)
            toAdd.put(l, o)
        else
            elements.put(l, o)
    }

    fun remove(l: T, o: K) {
        if (beingTraversed)
            toRemove.put(l, o)
        else
            elements.remove(l, o)
    }

    val size
    get() = elements.size + toAdd.size - toRemove.size

    fun forEach(f: (T, K) -> Unit) {
        beingTraversed = true
        elements.forEach(f)
        beingTraversed = false
        elements.putAll(toAdd)
        toAdd.clear()
        toRemove.forEach { t, k -> elements.remove(t) }
        toRemove.clear()
    }
}