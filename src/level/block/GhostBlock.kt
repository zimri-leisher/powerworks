package level.block

import graphics.Image
import graphics.RenderParams
import graphics.Renderer
import main.Game

class GhostBlock(xTile: Int, yTile: Int, type: BlockType) : Block(xTile, yTile, type, type.hitbox, false) {

    var placeable = getCollision(xPixel, yPixel) == null

    override fun render() {
        Renderer.renderTexture(type.getTexture(rotation), xPixel, yPixel, RenderParams(alpha = 0.4f))
        Renderer.renderTexture(if (placeable) Image.BLOCK_PLACEABLE else Image.BLOCK_NOT_PLACEABLE, xPixel, yPixel, type.widthTiles shl 4, type.heightTiles shl 4)
        if(Game.RENDER_HITBOXES)
            renderHitbox()
    }
}