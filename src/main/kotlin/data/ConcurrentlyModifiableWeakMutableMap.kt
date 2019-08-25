package data

class ConcurrentlyModifiableWeakMutableMap<K, V> {
    var beingTraversed = false

    val elements = WeakMutableMap<K, V>()

    val toAdd = mutableMapOf<K, V>()

    val toRemove = mutableListOf<K>()

    fun put(l: K, o: V) {
        println("adding $l, $o")
        if (beingTraversed)
            toAdd.put(l, o)
        else
            elements.put(l, o)
    }

    fun remove(l: K) {
        if (beingTraversed)
            toRemove.add(l)
        else
            elements.remove(l)
    }

    val size
        get() = elements.size + toAdd.size - toRemove.size

    fun forEach(f: (K, V) -> Unit) {
        beingTraversed = true
        elements.forEach(f)
        beingTraversed = false
        elements.putAll(toAdd)
        toAdd.clear()
        toRemove.forEach { t -> elements.remove(t) }
        toRemove.clear()
    }

    override fun toString() = elements.toString()

    fun clear() {
        elements.clear()
        toAdd.clear()
        toRemove.clear()
    }

    fun joinToString() = elements.joinToString()
}