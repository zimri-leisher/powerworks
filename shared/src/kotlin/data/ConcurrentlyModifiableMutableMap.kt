package data

import main.joinToString

class ConcurrentlyModifiableMutableMap<K, V> {

    var beingTraversed = false
        set(value) {
            if(field != value) {
                field = value
                if(field) {
                    elements.putAll(toAdd)
                    toAdd.clear()
                    toRemove.forEach { t -> elements.remove(t) }
                    toRemove.clear()
                }
            }
        }

    val elements = mutableMapOf<K, V>()

    val toAdd = mutableMapOf<K, V>()

    val toRemove = mutableListOf<K>()

    fun put(l: K, o: V) {
        if (beingTraversed)
            toAdd.put(l, o)
        else
            elements.put(l, o)
    }

    operator fun set(l: K, o: V) = put(l, o)

    fun remove(l: K) {
        if (beingTraversed)
            toRemove.add(l)
        else
            elements.remove(l)
    }

    val size
        get() = elements.size + toAdd.size - toRemove.size

    operator fun get(key: K): V? {
        if (key in toRemove)
            return null
        return elements[key] ?: toAdd[key]
    }

    fun forEach(f: (K, V) -> Unit) {
        beingTraversed = true
        elements.forEach(f)
        beingTraversed = false
    }

    operator fun iterator() = elements.iterator()

    override fun toString() = "[${elements.joinToString()}]"

    operator fun contains(key: K) = (key in elements || key in toAdd) && key !in toRemove

    fun clear() {
        elements.clear()
        toAdd.clear()
        toRemove.clear()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConcurrentlyModifiableMutableMap<*, *>

        if (elements != other.elements) return false
        if (toAdd != other.toAdd) return false
        if (toRemove != other.toRemove) return false

        return true
    }

    override fun hashCode(): Int {
        var result = elements.hashCode()
        result = 31 * result + toAdd.hashCode()
        result = 31 * result + toRemove.hashCode()
        return result
    }
}