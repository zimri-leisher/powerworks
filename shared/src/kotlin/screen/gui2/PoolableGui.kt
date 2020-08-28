package screen.gui2

interface PoolableGui {
    fun canDisplay(obj: Any?): Boolean
    fun display(obj: Any?)
    fun isDisplaying(obj: Any?): Boolean
}