package level.entity.robot

import graphics.ImageCollection
import level.Hitbox
import level.PhysicalLevelObjectTextures
import level.entity.EntityType
import network.User
import player.Player
import serialization.ObjectList
import java.util.*

class RobotType<T : Robot>(initializer: RobotType<T>.() -> Unit) : EntityType<T>() {

    init {
        initializer()
        mass = hitbox.width * hitbox.height * density
        ALL.add(this)
    }

    companion object {
        @ObjectList
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