package level.entity.robot

import graphics.ImageCollection
import level.Hitbox
import level.LevelManager
import level.LevelObjectTextures
import level.entity.EntityType
import network.User
import player.Player
import java.util.*

class RobotType<T : Robot>(initializer: RobotType<T>.() -> Unit) : EntityType<T>() {

    init {
        initializer()
        mass = hitbox.width * hitbox.height * density
        ALL.add(this)
    }

    companion object {

        val ALL = mutableListOf<RobotType<*>>()

        val STANDARD = RobotType<Robot> {
            instantiate = { x, y, rotation -> Robot(this, x, y, rotation) }
            textures = LevelObjectTextures(ImageCollection.ROBOT)
            hitbox = Hitbox.STANDARD_ROBOT
        }

        val BRAIN = RobotType<BrainRobot> {
            instantiate = { x, y, rotation -> BrainRobot(x, y, rotation, User(UUID.randomUUID(), "")) }
            textures = LevelObjectTextures(ImageCollection.ROBOT)
            hitbox = Hitbox.STANDARD_ROBOT
            damageable = false
        }
    }
}