package level.entity.robot

import graphics.ImageCollection
import level.Hitbox
import level.PhysicalLevelObjectTextures
import level.entity.EntityType

class RobotType<T : Robot>(initializer: RobotType<T>.() -> Unit) : EntityType<T>() {

    init {
        initializer()
        mass = hitbox.width * hitbox.height * density
        ALL.add(this)
    }

    companion object {

        val ALL = mutableListOf<RobotType<*>>()

        val STANDARD = RobotType<Robot> {
            textures = PhysicalLevelObjectTextures(ImageCollection.ROBOT)
            hitbox = Hitbox.STANDARD_ROBOT
        }

        val BRAIN = RobotType<BrainRobot> {
            textures = PhysicalLevelObjectTextures(ImageCollection.ROBOT)
            hitbox = Hitbox.STANDARD_ROBOT
            damageable = false
        }
    }
}