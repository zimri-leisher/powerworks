package level.entity.robot

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag
import graphics.Renderer
import graphics.text.TextRenderParams
import item.Inventory
import network.User

class BrainRobot(xPixel: Int, yPixel: Int, rotation: Int,
                 @Tag(28)
                 val user: User) : Robot(RobotType.BRAIN, xPixel, yPixel, rotation) {

    @Tag(29)
    val inventory = Inventory(8, 6)

    override fun render() {
        super.render()
        Renderer.renderText("${user.displayName}'s BR/AIN", xPixel - 8, yPixel + 16, params = TextRenderParams(size = 10))
    }
}