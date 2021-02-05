package level.pipe

import graphics.Image
import graphics.Renderer
import level.block.PipeBlockType
import main.DebugCode
import main.Game
import main.height
import main.width
import routing.FluidPipeNetwork
import routing.PipeNetwork
import serialization.Id

class FluidPipeBlock(xTile: Int, yTile: Int) : PipeBlock(PipeBlockType.FLUID_PIPE, xTile, yTile) {
    @Id(25)
    override var network: PipeNetwork = FluidPipeNetwork(level)

    override fun render() {
        Renderer.renderTexture(type.images[state]!!, x, y + 1)
        if (closedEnds[0])
            Renderer.renderTexture(Image.Block.PIPE_UP_CLOSE, x + 4, y + 17)
        if (closedEnds[1])
            Renderer.renderTexture(Image.Block.PIPE_RIGHT_CLOSE, x + 16, y + (18 - Image.Block.PIPE_RIGHT_CLOSE.height) / 2)
        if (closedEnds[2])
            Renderer.renderTexture(Image.Block.PIPE_DOWN_CLOSE, x + 4, y - 5)
        if (closedEnds[3])
            Renderer.renderTexture(Image.Block.PIPE_LEFT_CLOSE, x - Image.Block.PIPE_LEFT_CLOSE.width, y + (18 - Image.Block.PIPE_LEFT_CLOSE.height) / 2)
        if (nodeConnections[0].isNotEmpty())
            Renderer.renderTexture(Image.Block.PIPE_UP_CONNECT, x + 4, y + 17)
        if (Game.currentDebugCode == DebugCode.RENDER_HITBOXES)
            renderHitbox()
        if(health != type.maxHealth) {
            renderHealthBar()
        }
    }
}