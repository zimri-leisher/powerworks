package data

import java.lang.ref.WeakReference
import java.util.stream.Stream

class WeakMutableList<T> {
    var onAdd: WeakMutableList<T>.(T) -> Unit = {}
    var onRemove: WeakMutableList<T>.(T) -> Unit = {}
    private val list = mutableListOf<WeakReference<T>>()

    fun add(e: T): Boolean {
        val ret = list.add(WeakReference(e))
        onAdd(e)
        return ret
    }

    fun remove(e: T): Boolean {
        val i = list.iterator()
        for (t in i) {
            if (t.get() == e) {
                i.remove()
                onRemove(e)
                return true
            }
        }
        return false
    }

    private fun check() {
        val i = list.iterator()
        for (t in i) {
            if (t.get() == null) {
                i.remove()
            }
        }
    }

    fun forEach(f: (T) -> Unit) {
        check()
        list.forEach { f(it.get()!!) }
    }

    fun clear() {
        list.clear()
    }

    fun contains(first: T): Boolean {
        return list.any { it.get() == first }
    }

    fun sortBy(function: (T) -> Int) {
        list.sortBy { if(it.get() != null) function(it.get()!!) else 0 }
    }

    fun stream(): Stream<T> {
        check()
        return list.map { it.get()!! }.stream()
    }
}