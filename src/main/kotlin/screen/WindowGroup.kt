package screen

import screen.elements.GUIWindow

class WindowGroup(var layer: Int, val name: String) {

    private val _windows = mutableListOf<GUIWindow>()

    val windows = object : MutableList<GUIWindow> by _windows {
        override fun add(element: GUIWindow): Boolean {
            val result = _windows.add(element)
            sortBy { it.layer }
            return result
        }
    }

    init {
        ScreenManager.windowGroups.add(this)
    }

    /**
     * Gets the highest window matching the predicate
     */
    fun getTop(predicate: (GUIWindow) -> Boolean = { true }): GUIWindow? {
        return windows.stream().filter(predicate).max { o1, o2 -> o1.layer.compareTo(o2.layer) }.orElseGet { null }
    }

    /**
     * @param window the window to move to the highest layer
     */
    fun bringToTop(window: GUIWindow) {
        if (windows.contains(window)) {
            window.layer = windows.size + 1
        }
        windows.sortBy { it.layer }
        compressLayers()
    }

    private fun compressLayers() {
        var i = 0
        windows.stream().forEachOrdered {
            it.layer = i
            i++
        }
    }

    override fun toString() = name
}