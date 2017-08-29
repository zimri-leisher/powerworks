package screen

abstract class GUI(name: String, xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int) : GUIElement(null, name, xPixel, yPixel, widthPixels, heightPixels, 0) {
    fun poke() {}
}