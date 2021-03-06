package level.entity.robot

import graphics.Renderer
import graphics.text.TextRenderParams
import item.Inventory
import network.BrainRobotReference
import network.LevelObjectReference
import network.User
import serialization.Id

class BrainRobot(x: Int, y: Int, rotation: Int,
                 @Id(28)
                 var user: User) : Robot(RobotType.BRAIN, x, y, rotation) {

    @Id(29)
    val inventory = Inventory(8, 3)

    init {
        inventory.attachedLevelObject = this
        containers = listOf(inventory)
    }

    override fun render() {
        super.render()
        Renderer.renderText("${user.displayName}'s BRAIN", x - 8, y + 44, params = TextRenderParams(size = 10))
    }

    override fun toReference(): LevelObjectReference {
        return BrainRobotReference(this)
    }
}