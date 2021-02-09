package data

class ConcurrentlyModifiableWeakMutableList<T> {

    val size
        get() = elements.size + toAdd.size - toRemove.size

    var traversalDepth = 0

    val elements = WeakMutableList<T>()

    val toAdd = mutableListOf<T>()

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
        } else
            elements.remove(l)
    }

    fun contains(l: T) = !toRemove.contains(l) && (elements.contains(l) || toAdd.contains(l))

    fun <R : Comparable<R>>sortedBy(f: (T) -> R?) = elements.sortedBy(f)

    fun forEach(f: (T) -> Unit) {
        traversalDepth++
        elements.forEach(f)
        traversalDepth--
        if(traversalDepth == 0) {
            elements.addAll(toAdd)
            toAdd.clear()
            elements.removeAll(toRemove)
            toRemove.clear()
        }
    }

    override fun toString() = "[${elements.joinToString()}]"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConcurrentlyModifiableWeakMutableList<*>

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