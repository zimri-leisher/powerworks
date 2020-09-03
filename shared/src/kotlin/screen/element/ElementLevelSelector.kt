package screen.element

import com.badlogic.gdx.graphics.g2d.TextureRegion
import graphics.Image
import graphics.Renderer
import graphics.TextureRenderParams
import level.LevelInfo
import main.height
import main.width
import misc.Geometry
import misc.Coord
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
                levelPositions = value.mapKeys { (key, value) -> key to value }.mapValues { Coord(((width - 40) * Math.random()).toInt() + 40, ((height - 40) * Math.random()).toInt() + 40) }
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

    private var levelPositions: Map<Pair<UUID, LevelInfo>, Coord> = levels.mapKeys { (key, value) -> key to value }.mapValues { Coord(((width - 20) * Math.random()).toInt() + 20, ((height - 20) * Math.random()).toInt() + 20) }

    private var starPositions = List(40) { Coord((width.toDouble() * Math.random()).toInt(), (height.toDouble() * Math.random()).toInt()) to StarType.values().random() }

    override fun onChangeDimensions() {
        starPositions = List(40) { Coord((width.toDouble() * Math.random()).toInt(), (height.toDouble() * Math.random()).toInt()) to StarType.values().random() }
        levelPositions = levels.mapKeys { (key, value) -> key to value }.mapValues { Coord(((width - 20) * Math.random()).toInt() + 20, ((height - 20) * Math.random()).toInt() + 20) }
        super.onChangeDimensions()
    }

    private fun getLevelAt(x: Int, y: Int): Pair<UUID, LevelInfo>? {
        return levelPositions.entries.firstOrNull { (_, position) ->
            Geometry.intersects(position.x - Image.Gui.PLANET_NORMAL.width / 2, position.y - Image.Gui.PLANET_NORMAL.height / 2, Image.Gui.PLANET_NORMAL.width, Image.Gui.PLANET_NORMAL.height, x, y, 1, 1)
        }?.key
    }

    override fun update() {
        hoveringLevel = getLevelAt(Mouse.x - absoluteX, Mouse.y - absoluteY)
        super.update()
    }

    override fun onInteractOn(interaction: Interaction) {
        selectedLevel = hoveringLevel
        super.onInteractOn(interaction)
    }

    override fun render(params: TextureRenderParams?) {
        Renderer.pushClip(absoluteX, absoluteY, width, height)
        val actualParams = params ?: TextureRenderParams.DEFAULT
        Renderer.renderTexture(Image.Gui.BLACK_FILLER, absoluteX, absoluteY, width, height, actualParams)
        for ((position, type) in starPositions) {
            Renderer.renderTexture(type.texture, absoluteX + position.x - type.texture.width / 2, absoluteY + position.y - type.texture.height / 2, actualParams)
        }
        for ((pair, position) in levelPositions) {
            val (id, info) = pair
            if (pair == selectedLevel) {
                Renderer.renderTexture(Image.Gui.PLANET_NORMAL, absoluteX + position.x - Image.Gui.PLANET_NORMAL.width / 2, absoluteY + position.y - Image.Gui.PLANET_NORMAL.height / 2, actualParams)
                Renderer.renderEmptyRectangle(absoluteX + position.x - Image.Gui.PLANET_NORMAL.width / 2, absoluteY + position.y - Image.Gui.PLANET_NORMAL.height / 2, Image.Gui.PLANET_NORMAL.width, Image.Gui.PLANET_NORMAL.height, borderThickness = 2f)
                Renderer.renderText("${info.owner.displayName}'s level", absoluteX + position.x - Image.Gui.PLANET_NORMAL.width / 2, absoluteY + position.y + Image.Gui.PLANET_NORMAL.height / 2)
            } else if (pair == hoveringLevel) {
                Renderer.renderTexture(Image.Gui.PLANET_NORMAL, absoluteX + position.x - Image.Gui.PLANET_NORMAL.width / 2, absoluteY + position.y - Image.Gui.PLANET_NORMAL.height / 2, actualParams)
                Renderer.renderEmptyRectangle(absoluteX + position.x - Image.Gui.PLANET_NORMAL.width / 2, absoluteY + position.y - Image.Gui.PLANET_NORMAL.height / 2, Image.Gui.PLANET_NORMAL.width, Image.Gui.PLANET_NORMAL.height)
                Renderer.renderText("${info.owner.displayName}'s level", absoluteX + position.x - Image.Gui.PLANET_NORMAL.width / 2, absoluteY + position.y + Image.Gui.PLANET_NORMAL.height / 2)
            } else {
                Renderer.renderTexture(Image.Gui.PLANET_NORMAL, absoluteX + position.x - Image.Gui.PLANET_NORMAL.width / 2, absoluteY + position.y - Image.Gui.PLANET_NORMAL.height / 2, actualParams)
            }
        }
        Renderer.popClip()
        super.render(params)
    }
}