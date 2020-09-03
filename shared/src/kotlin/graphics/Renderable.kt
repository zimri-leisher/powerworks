package graphics

/**
 * Something that is able to be rendered, e.g. a [Texture] or an [Animation]
 */
abstract class Renderable {

    abstract val yOffset: Int
    abstract val xOffset: Int
    abstract val width: Int
    abstract val height: Int

    abstract fun render(x: Int, y: Int, width: Int = this.width, height: Int = this.height, keepAspect: Boolean = false, params: TextureRenderParams = TextureRenderParams.DEFAULT)
}