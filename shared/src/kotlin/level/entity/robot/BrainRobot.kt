package level.entity.robot

import graphics.Renderer
import graphics.text.TextRenderParams
import item.Inventory
import network.BrainRobotReference
import network.LevelObjectReference
import network.User
import player.PlayerManager
import serialization.Id

class BrainRobot(xPixel: Int, yPixel: Int, rotation: Int,
                 @Id(28)
                 var user: User) : Robot(RobotType.BRAIN, xPixel, yPixel, rotation) {

    @Id(29)
    val inventory = Inventory(8, 6)

    override fun render() {
        super.render()
        Renderer.renderText("${user.displayName}'s BR/AIN", xPixel - 8, yPixel + 44, params = TextRenderParams(size = 10))
    }

    override fun toReference(): LevelObjectReference {
        return BrainRobotReference(this)
    }
}