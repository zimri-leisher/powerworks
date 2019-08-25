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
    var requiresRender = false
    var textures = LevelObjectTextures(Image.Misc.ERROR)
        set(value) {
            field = value
            requiresRender = true
        }

    /**
     * Whether this object type should count in collisions. [LevelObject.onCollide] will still be called when its [Hitbox]
     * intersects another non-ghost hitbox, but it will not stop moving through.
     * It will be able to be [added][Level.add] to the [Level] even if it would collide with something, and it will not
     * be able to be set as the [Level.selectedLevelObject]
     */
    var ghost = false

    init {
        initializer()
    }

    companion object {

        val ERROR = LevelObjectType<LevelObject>()

    }
}