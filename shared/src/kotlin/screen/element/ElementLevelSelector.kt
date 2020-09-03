package screen.element

import com.badlogic.gdx.graphics.g2d.TextureRegion
import graphics.Image
import graphics.Renderer
import graphics.TextureRenderParams
import level.LevelInfo
import main.heightPixels
import main.widthPixels
import misc.Geometry
import misc.PixelCoord
import screen.gui.GuiElement
import screen.Interaction
import screen.mouse.Mouse
import java.util.*

class ElementLevelSelector(parent: GuiElement, levels: Map<UUID, LevelInfo>, var onSelectLevel: (UUID, LevelInfo) -> Unit) : GuiElement(parent) {

    private enum class StarType(val texture: TextureRegion) {
        REALLY_SMALL(Image.Gui.WHITE_FILLER), SMALL(Image.Gui.STAR_SMALL), MEDIUM(Image.Gui.STAR_MEDIUM), LARGE(Image.Gui.STAR_LARGE)
    }

    var levels = levels
        set(value) {
            if (field != value) {
                field = value
                levelPositions = value.mapKeys { (key, value) -> key to value }.mapValues { PixelCoord(((widthPixels - 40) * Math.random()).toInt() + 40, ((heightPixels - 40) * Math.random()).toInt() + 40) }
            }
        }

    private var hoveringLevel: Pair<UUID, LevelInfo>? = null
    var selectedLevel: Pair<UUID, LevelInfo>? = null
        set(value) {
            if (field != value) {
                field = value
                if(value != null) {
                    onSelectLevel(value.first, value.second)
                }
            }
        }

    private var levelPositions: Map<Pair<UUID, LevelInfo>, PixelCoord> = levels.mapKeys { (key, value) -> key to value }.mapValues { PixelCoord(((widthPixels - 20) * Math.random()).toInt() + 20, ((heightPixels - 20) * Math.random()).toInt() + 20) }

    private var starPositions = List(40) { PixelCoord((widthPixels.toDouble() * Math.random()).toInt(), (heightPixels.toDouble() * Math.random()).toInt()) to StarType.values().random() }

    override fun onChangeDimensions() {
        starPositions = List(40) { PixelCoord((widthPixels.toDouble() * Math.random()).toInt(), (heightPixels.toDouble() * Math.random()).toInt()) to StarType.values().random() }
        levelPositions = levels.mapKeys { (key, value) -> key to value }.mapValues { PixelCoord(((widthPixels - 20) * Math.random()).toInt() + 20, ((heightPixels - 20) * Math.random()).toInt() + 20) }
        super.onChangeDimensions()
    }

    private fun getLevelAt(xPixel: Int, yPixel: Int): Pair<UUID, LevelInfo>? {
        return levelPositions.entries.firstOrNull { (_, position) ->
            Geometry.intersects(position.xPixel - Image.Gui.PLANET_NORMAL.widthPixels / 2, position.yPixel - Image.Gui.PLANET_NORMAL.heightPixels / 2, Image.Gui.PLANET_NORMAL.widthPixels, Image.Gui.PLANET_NORMAL.heightPixels, xPixel, yPixel, 1, 1)
        }?.key
    }

    override fun update() {
        hoveringLevel = getLevelAt(Mouse.xPixel - absoluteXPixel, Mouse.yPixel - absoluteYPixel)
        super.update()
    }

    override fun onInteractOn(interaction: Interaction) {
        selectedLevel = hoveringLevel
        super.onInteractOn(interaction)
    }

    override fun render(params: TextureRenderParams?) {
        Renderer.pushClip(absoluteXPixel, absoluteYPixel, widthPixels, heightPixels)
        val actualParams = params ?: TextureRenderParams.DEFAULT
        Renderer.renderTexture(Image.Gui.BLACK_FILLER, absoluteXPixel, absoluteYPixel, widthPixels, heightPixels, actualParams)
        for ((position, type) in starPositions) {
            Renderer.renderTexture(type.texture, absoluteXPixel + position.xPixel - type.texture.widthPixels / 2, absoluteYPixel + position.yPixel - type.texture.heightPixels / 2, actualParams)
        }
        for ((pair, position) in levelPositions) {
            val (id, info) = pair
            if (pair == selectedLevel) {
                Renderer.renderTexture(Image.Gui.PLANET_NORMAL, absoluteXPixel + position.xPixel - Image.Gui.PLANET_NORMAL.widthPixels / 2, absoluteYPixel + position.yPixel - Image.Gui.PLANET_NORMAL.heightPixels / 2, actualParams)
                Renderer.renderEmptyRectangle(absoluteXPixel + position.xPixel - Image.Gui.PLANET_NORMAL.widthPixels / 2, absoluteYPixel + position.yPixel - Image.Gui.PLANET_NORMAL.heightPixels / 2, Image.Gui.PLANET_NORMAL.widthPixels, Image.Gui.PLANET_NORMAL.heightPixels, borderThickness = 2f)
                Renderer.renderText("${info.owner.displayName}'s level", absoluteXPixel + position.xPixel - Image.Gui.PLANET_NORMAL.widthPixels / 2, absoluteYPixel + position.yPixel + Image.Gui.PLANET_NORMAL.heightPixels / 2)
            } else if (pair == hoveringLevel) {
                Renderer.renderTexture(Image.Gui.PLANET_NORMAL, absoluteXPixel + position.xPixel - Image.Gui.PLANET_NORMAL.widthPixels / 2, absoluteYPixel + position.yPixel - Image.Gui.PLANET_NORMAL.heightPixels / 2, actualParams)
                Renderer.renderEmptyRectangle(absoluteXPixel + position.xPixel - Image.Gui.PLANET_NORMAL.widthPixels / 2, absoluteYPixel + position.yPixel - Image.Gui.PLANET_NORMAL.heightPixels / 2, Image.Gui.PLANET_NORMAL.widthPixels, Image.Gui.PLANET_NORMAL.heightPixels)
                Renderer.renderText("${info.owner.displayName}'s level", absoluteXPixel + position.xPixel - Image.Gui.PLANET_NORMAL.widthPixels / 2, absoluteYPixel + position.yPixel + Image.Gui.PLANET_NORMAL.heightPixels / 2)
            } else {
                Renderer.renderTexture(Image.Gui.PLANET_NORMAL, absoluteXPixel + position.xPixel - Image.Gui.PLANET_NORMAL.widthPixels / 2, absoluteYPixel + position.yPixel - Image.Gui.PLANET_NORMAL.heightPixels / 2, actualParams)
            }
        }
        Renderer.popClip()
        super.render(params)
    }
}