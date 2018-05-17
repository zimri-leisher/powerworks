package level

import graphics.Texture

data class LevelObjectTexture(val texture: Texture, val xPixelOffset: Int = 0, val yPixelOffset: Int = 0)

class LevelObjectTextures(private vararg val textures: LevelObjectTexture) {
    operator fun get(i: Int) = textures[Math.min(i, textures.lastIndex)]
    operator fun iterator() = textures.iterator()
}