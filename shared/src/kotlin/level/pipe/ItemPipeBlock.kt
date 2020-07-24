package level.pipe

import graphics.Image
import graphics.Renderer
import level.block.PipeBlockType
import main.DebugCode
import main.Game
import main.heightPixels
import main.widthPixels
import routing.ItemPipeNetwork
import routing.PipeNetwork
import serialization.Id

class ItemPipeBlock(xTile: Int, yTile: Int) : PipeBlock(PipeBlockType.ITEM_PIPE, xTile, yTile) {
    @Id(25)
    override var network: PipeNetwork = ItemPipeNetwork(level)

    override fun render() {
        val texture = type.images[state]!!
        Renderer.renderTexture(texture, xPixel, yPixel)
        if (closedEnds[0])
            Renderer.renderTexture(Image.Block.TUBE_UP_CLOSE, xPixel, yPixel + 16)
        if (closedEnds[1])
            Renderer.renderTexture(Image.Block.TUBE_RIGHT_CLOSE, xPixel + 16, yPixel + 20 - Image.Block.TUBE_RIGHT_CLOSE.heightPixels)
        if (closedEnds[2])
            Renderer.renderTexture(Image.Block.TUBE_DOWN_CLOSE, xPixel, yPixel + 14 - Image.Block.TUBE_DOWN_CLOSE.heightPixels)
        if (closedEnds[3])
            Renderer.renderTexture(Image.Block.TUBE_LEFT_CLOSE, xPixel - Image.Block.TUBE_LEFT_CLOSE.widthPixels, yPixel + 20 - Image.Block.TUBE_LEFT_CLOSE.heightPixels)
        if (nodeConnections[0].isNotEmpty())
            Renderer.renderTexture(Image.Block.TUBE_UP_CONNECT, xPixel + 1, yPixel + 19)
        if (Game.currentDebugCode == DebugCode.RENDER_HITBOXES)
            renderHitbox()
    }
}