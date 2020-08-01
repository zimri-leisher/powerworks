package data

import java.lang.ref.WeakReference
import java.util.*
import java.util.stream.Stream

class WeakMutableList<T> {
    var onAdd: WeakMutableList<T>.(T) -> Unit = {}
    var onRemove: WeakMutableList<T>.(T) -> Unit = {}
    private val list = mutableListOf<WeakReference<T>>()
    val size: Int
        get() = list.size

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
        var i = list.iterator()
        var retry = true
        while (retry) {
            retry = false
            try {
                for (t in i) {
                    if (t.get() == null) {
                        i.remove()
                    }
                }
            } catch (e: ConcurrentModificationException) {
                println("Concurrent modification exception in weak mutable list")
                i = list.iterator()
                retry = true
            }
        }
    }

    fun forEach(f: (T) -> Unit) {
        check()
        list.forEach {
            val o = it.get()
            try {
                o!!
            } catch (e: KotlinNullPointerException) {
                println("GC'd 1")
            }
            if (o != null) {
                f(o)
            }
        }
    }

    fun forEachBackwards(f: (T) -> Unit) {
        check()
        list.reversed().forEach {
            val o = it.get()
            try {
                o!!
            } catch (e: KotlinNullPointerException) {
                println("GC'd -1")
            }
            if (o != null) {
                f(o)
            }
        }
    }

    fun clear() {
        list.clear()
    }

    fun contains(first: T): Boolean {
        return list.any { it.get() == first }
    }

    fun lastOrNull(f: (T) -> Boolean): T? {
        check()
        return list.lastOrNull { f(it.get()!!) }?.get()
    }

    fun sortBy(function: (T) -> Int) {
        list.sortBy { if (it.get() != null) function(it.get()!!) else 0 }
    }

    fun addAll(f: List<T>) {
        f.forEach { add(it) }
    }

    fun removeAll(f: List<T>) {
        f.forEach { remove(it) }
    }

    operator fun iterator() = object : MutableIterator<T> {
        var index = 0

        override fun hasNext(): Boolean {
            if (index + 1 > list.lastIndex)
                return false
            var next = list[index + 1]
            while (next.get() == null) {
                index++
                if (index + 1 > list.lastIndex)
                    return false
                next = list[index + 1]
            }
            return true
        }

        override fun next(): T {
            index++
            var next = list[index]
            while (next.get() == null) {
                index++
                next = list[index]
            }
            val o = next.get()
            try {
                o!!
            } catch (e: KotlinNullPointerException) {
                println("GC'd 2")
                if (index + 1 > list.lastIndex) {
                    index = 0
                    // this will go back down till it finds something that hasnt been removed
                }
                return next()
            }
            return o
        }

        override fun remove() {
        }

    }

    fun stream(): Stream<T> {
        check()
        return list.map { it.get()!! }.stream()
    }

    fun filter(f: (T) -> Boolean) = list.filter { if (it.get() == null) false else f(it.get()!!) }.map { it.get()!! }

    fun isNotEmpty(): Boolean {
        check()
        return list.isNotEmpty()
    }

    fun <R : Comparable<R>> sortedBy(f: (T) -> R?): List<T> {
        check()
        return list.map { it.get()!! }.sortedBy(f)
    }

    fun joinToString(): String {
        check()
        return list.joinToString { it.get().toString() }
    }
}