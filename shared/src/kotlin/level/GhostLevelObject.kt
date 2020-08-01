package level

import graphics.Image
import graphics.Renderer
import level.block.PipeBlockType
import level.pipe.PipeState
import main.heightPixels
import main.widthPixels
import misc.Geometry
import network.GhostLevelObjectReference

class GhostLevelObject(type: LevelObjectType<*>, xPixel: Int, yPixel: Int, rotation: Int) : LevelObject(type, xPixel, yPixel, rotation) {
    private constructor() : this(LevelObjectType.ERROR, 0, 0, 0)

    lateinit var pipeState: PipeState
        private set
    val pipeClosedEnds: Array<Boolean>
        get() = pipeState.closedEnds

    override fun onAddToLevel() {
        super.onAddToLevel()
        if (type is PipeBlockType) {
            val dirs = arrayOf(false, false, false, false)
            for (i in 0..3) {
                val adjXTile = xTile + Geometry.getXSign(i)
                val adjYTile = yTile + Geometry.getYSign(i)
                if (level.getBlockAt(adjXTile, adjYTile)?.type == type
                        || level.data.ghostObjects.any { it.type == type && it.xTile == adjXTile && it.yTile == adjYTile }) {
                    dirs[i] = true
                }
            }
            pipeState = PipeState.getState(dirs)
        }
    }

    override fun render() {
        if (type is PipeBlockType) {
            val texture = type.images[pipeState]!!
            Renderer.renderTexture(texture, xPixel, yPixel)
            if (pipeClosedEnds[0])
                Renderer.renderTexture(Image.Block.TUBE_UP_CLOSE, xPixel, yPixel + 16)
            if (pipeClosedEnds[1])
                Renderer.renderTexture(Image.Block.TUBE_RIGHT_CLOSE, xPixel + 16, yPixel + 20 - Image.Block.TUBE_RIGHT_CLOSE.heightPixels)
            if (pipeClosedEnds[2])
                Renderer.renderTexture(Image.Block.TUBE_DOWN_CLOSE, xPixel, yPixel + 14 - Image.Block.TUBE_DOWN_CLOSE.heightPixels)
            if (pipeClosedEnds[3])
                Renderer.renderTexture(Image.Block.TUBE_LEFT_CLOSE, xPixel - Image.Block.TUBE_LEFT_CLOSE.widthPixels, yPixel + 20 - Image.Block.TUBE_LEFT_CLOSE.heightPixels)
        } else if (type != LevelObjectType.DROPPED_ITEM) {
            super.render()
        }
    }

    override fun toReference() = GhostLevelObjectReference(this)
}