package level.entity.robot

import graphics.ImageCollection
import level.Hitbox
import level.LevelObjectTextures
import level.entity.EntityType

class RobotType<T : Robot>(initializer: RobotType<T>.() -> Unit) : EntityType<T>() {

    init {
        initializer()
    }

    companion object {
        val STANDARD = RobotType<Robot> {
            instantiate = {xPixel, yPixel, rotation -> Robot(this, xPixel, yPixel, rotation) }
            textures = LevelObjectTextures(ImageCollection.ROBOT)
            hitbox = Hitbox.STANDARD_ROBOT
        }
    }
}