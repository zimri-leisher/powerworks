package level.entity.robot

import graphics.Renderer
import graphics.text.TextRenderParams
import item.Inventory
import level.Level
import network.BrainRobotReference
import network.LevelObjectReference
import network.MovingObjectReference
import network.User
import serialization.AsReference
import serialization.Id
import java.util.*

class BrainRobot(
    x: Int, y: Int,
    @Id(28)
    var user: User
) : Robot(RobotType.BRAIN, x, y) {

    private constructor() : this(0, 0, User(UUID.randomUUID(), ""))

    @Id(29)
    val inventory = Inventory(8, 3)

    override fun afterAddToLevel(oldLevel: Level) {
        super.afterAddToLevel(oldLevel)
        if(inventory.inLevel) {
            inventory.level.remove(inventory)
        }
        level.add(inventory)
    }

    override fun render() {
        super.render()
        Renderer.renderText("${user.displayName}'s BRAIN", x - 8, y + 44, params = TextRenderParams(size = 10))
    }

    override fun toReference(): BrainRobotReference {
        return BrainRobotReference(this)
    }
}