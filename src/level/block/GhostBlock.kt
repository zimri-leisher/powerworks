package level.block

import graphics.Images
import graphics.RenderParams
import graphics.Renderer
import level.Hitbox
import main.Game

class GhostBlock(xTile: Int, yTile: Int, type: BlockType) : Block(xTile, yTile, type, Hitbox.NONE, false) {

    val placeable = !getCollision(0, 0)

    override fun render() {
        Renderer.renderTexture(type.getTexture(rotation), xPixel, yPixel, RenderParams(alpha = 0.4f))
        Renderer.renderTexture(if (placeable) Images.BLOCK_PLACEABLE else Images.BLOCK_NOT_PLACEABLE, xPixel, yPixel, type.widthTiles shl 4, type.heightTiles shl 4)
        if(Game.RENDER_HITBOXES)
            renderHitbox()
    }
}