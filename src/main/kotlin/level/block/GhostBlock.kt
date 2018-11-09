package level.block

import com.badlogic.gdx.graphics.Color
import graphics.Image
import graphics.Renderer
import graphics.TextureRenderParams
import main.DebugCode
import main.Game
import main.toColor

class GhostBlock(type: BlockType<*>, xTile: Int, yTile: Int, rotation: Int) : Block(type, xTile, yTile, rotation) {
    private var placeable = getCollision(xPixel, yPixel) == null

    init {
        requiresUpdate = false
    }

    override fun render() {
        type.textures.render(this, TextureRenderParams(color = Color(1f, 1f, 1f, 0.4f)))
        Renderer.renderEmptyRectangle(xPixel, yPixel, type.widthTiles shl 4, type.heightTiles shl 4, params = TextureRenderParams(color = toColor(if (placeable) 0x04C900 else 0xC90004, 0.3f)))
        Renderer.renderTextureKeepAspect(Image.Misc.ARROW, xPixel, yPixel, type.widthTiles shl 4, type.heightTiles shl 4, TextureRenderParams(rotation = 90f * rotation))
        if (Game.currentDebugCode == DebugCode.RENDER_HITBOXES) {
            renderHitbox()
        }
    }
}