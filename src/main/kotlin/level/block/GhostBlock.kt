package level.block

import graphics.Image
import graphics.RenderParams
import graphics.Renderer
import main.DebugCode
import main.Game

class GhostBlock(type: BlockType<*>, xTile: Int, yTile: Int, rotation: Int) : Block(type, xTile, yTile, rotation) {
    var placeable = getCollision(xPixel, yPixel) == null

    init {
        requiresUpdate = false
    }

    override fun render() {
        val texture = type.textures[rotation]
        Renderer.renderTexture(texture.texture, xPixel - texture.xPixelOffset, yPixel - texture.yPixelOffset, RenderParams(alpha = 0.4f))
        Renderer.renderEmptyRectangle(xPixel, yPixel, type.widthTiles shl 4, type.heightTiles shl 4, if (placeable) 0x04C900 else 0xC90004, RenderParams(alpha = 0.45f))
        Renderer.renderTexture(Image.Misc.ARROW, xPixel, yPixel, type.widthTiles shl 4, type.heightTiles shl 4, RenderParams(rotation = 90f * rotation))
        if (Game.currentDebugCode == DebugCode.RENDER_HITBOXES)
            renderHitbox()
    }
}