package level.block

import graphics.RenderParams
import graphics.Renderer
import main.Game

class GhostBlock(xTile: Int, yTile: Int, type: BlockType) : Block(type, yTile, xTile, type.hitbox, false) {

    var placeable = getCollision(xPixel, yPixel) == null

    override fun render() {
        Renderer.renderTexture(type.getTexture(rotation), xPixel - type.textureXPixelOffset, yPixel - type.textureYPixelOffset, RenderParams(alpha = 0.4f))
        Renderer.renderEmptyRectangle(xPixel, yPixel, type.widthTiles shl 4, type.heightTiles shl 4, if(placeable) 0x04C900 else 0xC90004, .45f)
        if(Game.RENDER_HITBOXES)
            renderHitbox()
    }
}