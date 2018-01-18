package misc

import java.lang.ref.WeakReference

class WeakMutableList<T> {
    private val list = mutableListOf<WeakReference<T>>()

    fun add(e: T): Boolean {
        return list.add(WeakReference(e))
    }

    fun remove(e: T): Boolean {
        val i = list.iterator()
        for (t in i) {
            if (t.get() == e) {
                i.remove()
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
        synchronized(this, {
            check()
            // can't be any nulls now
            list.forEach { f(it.get()!!) }
        })
    }

    fun clear() {
        list.clear()
    }

    fun contains(first: T): Boolean {
        return list.any { it.get() == first }
    }
}