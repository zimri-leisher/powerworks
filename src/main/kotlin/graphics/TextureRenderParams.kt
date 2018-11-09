package graphics

import com.badlogic.gdx.graphics.Color

data class TextureRenderParams(
        /**
         * The amount to scale the x axis. This will scale from the origin of the rendered texture
         */
        var scaleX: Float = 1.0f,
        /**
         * The amount to scale the y axis. This will scale from the origin of the rendered texture
         */
        var scaleY: Float = 1.0f,
        var color: Color = Color(1f, 1f, 1f, 1f),
        var brightness: Float = 1f,
        /**
         * Degrees rotation
         */
        var rotation: Float = 0f) {

    fun combine(other: TextureRenderParams): TextureRenderParams {
        return TextureRenderParams(scaleX * other.scaleX, scaleY * other.scaleY, color.cpy().mul(other.color), brightness * other.brightness, (rotation + other.rotation) % 360)
    }

    companion object {
        val DEFAULT = TextureRenderParams()
    }
}