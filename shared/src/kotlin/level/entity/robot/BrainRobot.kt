package level.entity.robot

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag
import graphics.Renderer
import graphics.text.TextRenderParams
import item.Inventory
import network.User
import player.Player
import player.PlayerManager
import serialization.Id
import java.util.*

class BrainRobot(xPixel: Int, yPixel: Int, rotation: Int,
                 @Id(28)
                 var user: User) : Robot(RobotType.BRAIN, xPixel, yPixel, rotation) {

    val player get() = PlayerManager.getPlayer(user)

    @Id(29)
    val inventory = Inventory(8, 6)

    override fun render() {
        super.render()
        Renderer.renderText("${player.user.displayName}'s BR/AIN", xPixel - 8, yPixel + 44, params = TextRenderParams(size = 10))
    }
}