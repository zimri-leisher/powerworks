package screen

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

    fun bringToTop(window: GUIWindow) {
        if (windows.contains(window)) {
            window.layer = windows.size + 1
        }
        windows.sortBy { it.layer }
        compressLayers()
    }

    fun compressLayers() {
        var i = 0
        windows.stream().forEachOrdered {
            it.layer = i
            i++
        }
    }

    override fun toString() = name
}