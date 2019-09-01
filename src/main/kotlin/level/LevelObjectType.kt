package level

import graphics.Image
import screen.Camera

private var nextId = 0

open class LevelObjectType<T : LevelObject>(initializer: LevelObjectType<T>.() -> Unit = {}) {
    val id = nextId++
    /**
     * 1: x pixel
     * 2: y pixel
     * 3: rotation
     */
    var instantiate: (xPixel: Int, yPixel: Int, rotation: Int) -> T = { _, _, _ -> throw Exception("Level object type failed to specify an adequate instantiator function") }
    var hitbox = Hitbox.NONE
    var requiresUpdate = false
    var textures = LevelObjectTextures(Image.Misc.ERROR)

    init {
        initializer()
    }

    companion object {

        val ERROR = LevelObjectType<LevelObject>()

    }
}