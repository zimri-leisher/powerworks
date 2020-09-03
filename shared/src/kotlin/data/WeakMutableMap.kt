package data

import main.joinToString
import java.lang.ref.WeakReference
import kotlin.collections.Map.Entry

/**
 * A [Map] where keys are stored as [WeakReference]s, meaning if this map is the last reference to the key, that key will
 * be able to be garbage collected. Every time a function is called that is meant to take values from the map, the
 * map will be checked for garbage collected keys and the corresponding entries will be removed
 * These cannot have null as their key, as it can't distinguish between an intentional null and a null that was because
 * of a garbage collection
 */
class WeakMutableMap<K, V> {
    private val map = mutableMapOf<WeakReference<K>, V>()
    val size: Int
        get() = map.size

    fun put(key: K, value: V): V? {
        return map.put(WeakReference(key), value)
    }

    fun remove(key: K): V? {
        val i = map.iterator()
        for ((k, v) in i) {
            if (k.get() == key) {
                i.remove()
                return v
            } else if (k.get() == null) {
                i.remove()
            }
        }
        return null
    }

    private fun check() {
        val i = map.iterator()
        for ((k, v) in i) {
            if (k.get() == null) {
                i.remove()
            }
        }
    }

    operator fun get(key: K): V? {
        for ((k, v) in map) {
            if (k.get() == key) {
                return v
            }
        }
        return null
    }

    fun forEach(f: (K, V) -> Unit) {
        check()
        map.forEach { (k, v) ->
            // prevents GC from removing while iterating
            val o = k.get()
            try {
                o!!
            } catch (e: KotlinNullPointerException) {
                println("GC'd 3")
            }
            if (o != null) {
                f(o, v)
            }
        }
    }

    fun clear() {
        map.clear()
    }

    fun contains(key: K): Boolean {
        return map.any { it.key.get() == key }
    }

    fun putAll(f: Map<K, V>) {
        f.forEach { put(it.key, it.value) }
    }

    fun removeAll(f: Map<K, V>) {
        f.forEach { remove(it.key) }
    }

    /*
    operator fun iterator() = object : MutableIterator<MutableMap.MutableEntry<K, V>> {

        private var _iterator = map.iterator()

        var index = 0

        override fun hasNext() = _iterator.hasNext()

        override fun next(): MutableMap.MutableEntry<K, V> {
            val next = _iterator.next()
            val newNext =
        }

        override fun remove() {
        }

    }
     */

    fun filter(f: (K) -> Boolean): List<Pair<K, V>> {
        check()
        return map.filter { if (it.key.get() == null) false else f(it.key.get()!!) }.map { it.key.get()!! to it.value }
    }

    fun isNotEmpty(): Boolean {
        check()
        return map.isNotEmpty()
    }

    fun joinToString(): String {
        return map.toList().joinToString { "(${it.first.get().toString()}, ${it.second}" }
    }
}