package level.living.robot

import graphics.ImageCollection
import level.Hitbox
import level.LevelObjectTextures
import level.living.LivingType

class RobotType<T : Robot>(initializer: RobotType<T>.() -> Unit) : LivingType<T>() {

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