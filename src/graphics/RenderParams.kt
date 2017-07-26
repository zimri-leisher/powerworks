package graphics

class RenderParams(val scale: Double = 1.0, val scaleWidth: Double = 1.0, val scaleHeight: Double = 1.0, val alpha: Double = 1.0, val xPixelOffset: Int = 0, val yPixelOffset: Int = 0, val rotation: Int = 0, val renderToLevel: Boolean = false) {
    fun combine(other: RenderParams): RenderParams {
        return RenderParams(scale * other.scale, scaleWidth * other.scaleWidth, scaleHeight * other.scaleHeight, alpha * other.alpha, xPixelOffset + other.xPixelOffset, yPixelOffset + other.yPixelOffset, (rotation + other.rotation) % 4, renderToLevel || other.renderToLevel)
    }
}