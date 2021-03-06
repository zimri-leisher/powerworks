package data

import serialization.Id

class ConcurrentlyModifiableMutableList<T>(
        @Id(1)
        val elements: MutableList<T> = mutableListOf()) {

    @Id(2)
    var traversalDepth = 0

    @Id(3)
    val toAdd = mutableListOf<T>()

    @Id(4)
    val toRemove = mutableListOf<T>()

    fun add(l: T) {
        if (traversalDepth > 0)
            toAdd.add(l)
        else
            elements.add(l)
    }

    fun remove(l: T) {
        if (traversalDepth > 0) {
            toRemove.add(l)
        } else {
            elements.remove(l)
        }
    }

    operator fun contains(l: T) = !toRemove.contains(l) && (elements.contains(l) || toAdd.contains(l))

    val size: Int
        get() {
            if (traversalDepth == 0) return elements.size
            var currentSize = elements.size
            for (removing in toRemove) {
                if (removing in elements || removing in toAdd) {
                    currentSize--
                }
            }
            for (adding in toAdd) {
                if (adding !in elements || adding !in toRemove) {
                    currentSize++
                }
            }
            return currentSize
        }

    fun forEach(f: (T) -> Unit) {
        startTraversing()
        elements.forEach(f)
        endTraversing()
    }

    fun startTraversing() {
        traversalDepth++
    }

    fun endTraversing() {
        traversalDepth--
        if(traversalDepth == 0) {
            elements.addAll(toAdd)
            toAdd.clear()
            elements.removeAll(toRemove)
            toRemove.clear()
        }
    }

    fun filter(pred: (T) -> Boolean): ConcurrentlyModifiableMutableList<T> {
        val elements = mutableListOf<T>()
        forEach { if (pred(it)) elements.add(it) }
        return ConcurrentlyModifiableMutableList(elements)
    }

    fun toMutableSet(): MutableSet<T> {
        val elements = mutableSetOf<T>()
        elements.addAll(this.elements)
        if (traversalDepth > 0) {
            elements.addAll(toAdd)
            elements.removeAll(toRemove)
        }
        return elements
    }

    operator fun iterator() = elements.iterator()

    fun isEmpty() = size == 0
    fun isNotEmpty() = !isEmpty()

    override fun toString() = "[${elements.joinToString()}]"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConcurrentlyModifiableMutableList<*>

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