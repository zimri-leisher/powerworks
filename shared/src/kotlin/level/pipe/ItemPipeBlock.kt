package level.pipe

import graphics.Image
import graphics.Renderer
import level.block.PipeBlockType
import main.DebugCode
import main.Game
import main.height
import main.width
import routing.ItemPipeNetwork
import routing.PipeNetwork
import serialization.Id

class ItemPipeBlock(xTile: Int, yTile: Int) : PipeBlock(PipeBlockType.ITEM_PIPE, xTile, yTile) {
    @Id(25)
    override var network: PipeNetwork = ItemPipeNetwork(level)

    override fun render() {
        val texture = type.images[state]!!
        Renderer.renderTexture(texture, x, y)
        if (closedEnds[0])
            Renderer.renderTexture(Image.Block.TUBE_UP_CLOSE, x, y + 16)
        if (closedEnds[1])
            Renderer.renderTexture(Image.Block.TUBE_RIGHT_CLOSE, x + 16, y + 20 - Image.Block.TUBE_RIGHT_CLOSE.height)
        if (closedEnds[2])
            Renderer.renderTexture(Image.Block.TUBE_DOWN_CLOSE, x, y + 14 - Image.Block.TUBE_DOWN_CLOSE.height)
        if (closedEnds[3])
            Renderer.renderTexture(Image.Block.TUBE_LEFT_CLOSE, x - Image.Block.TUBE_LEFT_CLOSE.width, y + 20 - Image.Block.TUBE_LEFT_CLOSE.height)
        if (nodeConnections[0].isNotEmpty())
            Renderer.renderTexture(Image.Block.TUBE_UP_CONNECT, x + 1, y + 19)
        if (Game.currentDebugCode == DebugCode.RENDER_HITBOXES)
            renderHitbox()
        if(health != type.maxHealth) {
            renderHealthBar()
        }
    }
}