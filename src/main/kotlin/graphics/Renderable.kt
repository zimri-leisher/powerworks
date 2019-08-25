package graphics

/**
 * Something that is able to be rendered, e.g. a [Texture] or an [Animation]
 */
abstract class Renderable {
    abstract val yPixelOffset: Int
    abstract val xPixelOffset: Int
    abstract val widthPixels: Int
    abstract val heightPixels: Int

    abstract fun render(xPixel: Int, yPixel: Int, widthPixels: Int = this.widthPixels, heightPixels: Int = this.heightPixels, keepAspect: Boolean = false, params: TextureRenderParams = TextureRenderParams.DEFAULT)
}