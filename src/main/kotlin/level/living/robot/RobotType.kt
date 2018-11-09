package level.living.robot

import graphics.ImageCollection
import level.LevelObjectTextures
import level.living.LivingType

class RobotType<T : Robot>(initializer: RobotType<T>.() -> Unit) : LivingType<T>() {

    init {
        initializer()
    }

    companion object {
        val STANDARD = RobotType<Robot> {
            textures = LevelObjectTextures(ImageCollection.ROBOT)
        }
    }
}