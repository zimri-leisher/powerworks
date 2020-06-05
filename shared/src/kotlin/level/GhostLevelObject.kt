package level

import network.GhostLevelObjectReference

class GhostLevelObject(type: LevelObjectType<*>, xPixel: Int, yPixel: Int, rotation: Int) : LevelObject(type, xPixel, yPixel, rotation) {
    private constructor() : this(LevelObjectType.ERROR, 0, 0, 0)

    override fun toReference() = GhostLevelObjectReference(this)
}