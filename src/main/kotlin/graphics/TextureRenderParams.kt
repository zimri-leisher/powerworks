package graphics

data class TextureRenderParams(
                    /**
                     * The amount to scale width and height
                     */
                    var scale: Float = 1.0f,
                    /**
                     * The amount to scale width
                     */
                    var scaleWidth: Float = 1.0f,
                    /**
                     * The amount to scale height
                     */
                    var scaleHeight: Float = 1.0f,
                    /**
                     * The alpha multiplier
                     */
                    var alpha: Float = 1.0f,
                    /**
                     * Amount of x pixels to be added when rendering
                     */
                    var xPixelOffset: Int = 0,
                    /**
                     * Amount of y pixels to be added when rendering
                     */
                    var yPixelOffset: Int = 0,
                    /**
                     * Degrees rotation
                     */
                    var rotation: Float = 0f) {
    fun combine(other: TextureRenderParams): TextureRenderParams {
        return TextureRenderParams(scale * other.scale, scaleWidth * other.scaleWidth, scaleHeight * other.scaleHeight, alpha * other.alpha, xPixelOffset + other.xPixelOffset, yPixelOffset + other.yPixelOffset, (rotation + other.rotation) % 360)
    }
}