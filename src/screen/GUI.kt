package screen

abstract class GUI(name: String, xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int, layer: Int = 0) : GUIElement(null, name, xPixel, yPixel, widthPixels, heightPixels, layer) {
    fun poke() {}
}