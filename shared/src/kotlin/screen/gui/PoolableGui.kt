package screen.gui

interface PoolableGui {
    fun canDisplay(obj: Any?): Boolean
    fun display(obj: Any?)
    fun isDisplaying(obj: Any?): Boolean
}