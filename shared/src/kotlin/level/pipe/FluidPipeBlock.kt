package level.pipe

import graphics.Image
import graphics.Renderer
import level.block.PipeBlockType
import main.DebugCode
import main.Game
import main.heightPixels
import main.widthPixels
import routing.FluidPipeNetwork
import routing.PipeNetwork
import serialization.Id

class FluidPipeBlock(xTile: Int, yTile: Int) : PipeBlock(PipeBlockType.FLUID_PIPE, xTile, yTile) {
    @Id(25)
    override var network: PipeNetwork = FluidPipeNetwork(level)

    override fun render() {
        Renderer.renderTexture(type.images[state]!!, xPixel, yPixel + 1)
        if (closedEnds[0])
            Renderer.renderTexture(Image.Block.PIPE_UP_CLOSE, xPixel + 4, yPixel + 17)
        if (closedEnds[1])
            Renderer.renderTexture(Image.Block.PIPE_RIGHT_CLOSE, xPixel + 16, yPixel + (18 - Image.Block.PIPE_RIGHT_CLOSE.heightPixels) / 2)
        if (closedEnds[2])
            Renderer.renderTexture(Image.Block.PIPE_DOWN_CLOSE, xPixel + 4, yPixel - 5)
        if (closedEnds[3])
            Renderer.renderTexture(Image.Block.PIPE_LEFT_CLOSE, xPixel - Image.Block.PIPE_LEFT_CLOSE.widthPixels, yPixel + (18 - Image.Block.PIPE_LEFT_CLOSE.heightPixels) / 2)
        if (nodeConnections[0].isNotEmpty())
            Renderer.renderTexture(Image.Block.PIPE_UP_CONNECT, xPixel + 4, yPixel + 17)
        if (Game.currentDebugCode == DebugCode.RENDER_HITBOXES)
            renderHitbox()
    }
}