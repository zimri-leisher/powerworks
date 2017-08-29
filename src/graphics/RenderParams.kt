package graphics

class RenderParams(var scale: Float = 1.0f, var scaleWidth: Float = 1.0f, var scaleHeight: Float = 1.0f, var alpha: Float = 1.0f, var xPixelOffset: Int = 0, var yPixelOffset: Int = 0, var rotation: Int = 0) {
    fun combine(other: RenderParams): RenderParams {
        return RenderParams(scale * other.scale, scaleWidth * other.scaleWidth, scaleHeight * other.scaleHeight, alpha * other.alpha, xPixelOffset + other.xPixelOffset, yPixelOffset + other.yPixelOffset, (rotation + other.rotation) % 4)
    }
}