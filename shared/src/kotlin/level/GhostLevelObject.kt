package level

import graphics.Image
import graphics.Renderer
import level.block.PipeBlockType
import level.pipe.PipeState
import main.height
import main.width
import misc.Geometry
import network.GhostLevelObjectReference

class GhostLevelObject(type: PhysicalLevelObjectType<*>, x: Int, y: Int) : PhysicalLevelObject(type, x, y) {
    private constructor() : this(PhysicalLevelObjectType.ERROR, 0, 0)

    lateinit var pipeState: PipeState
        private set
    val pipeClosedEnds: Array<Boolean>
        get() = pipeState.closedEnds

    override fun afterAddToLevel(oldLevel: Level) {
        super.afterAddToLevel(oldLevel)
        if (type is PipeBlockType) {
            val dirs = arrayOf(false, false, false, false)
            for (i in 0..3) {
                val adjXTile = xTile + Geometry.getXSign(i)
                val adjYTile = yTile + Geometry.getYSign(i)
                if (level.getBlockAtTile(adjXTile, adjYTile)?.type == type
                        || level.data.ghostObjects.any { it.type == type && it.xTile == adjXTile && it.yTile == adjYTile }) {
                    dirs[i] = true
                }
            }
            pipeState = PipeState.getState(dirs[0], dirs[1], dirs[2], dirs[3])
        }
    }

    override fun render() {
        if (type is PipeBlockType) {
            val texture = type.images[pipeState]!!
            Renderer.renderTexture(texture, x, y)
            if (pipeClosedEnds[0])
                Renderer.renderTexture(Image.Block.TUBE_UP_CLOSE, x, y + 16)
            if (pipeClosedEnds[1])
                Renderer.renderTexture(Image.Block.TUBE_RIGHT_CLOSE, x + 16, y + 20 - Image.Block.TUBE_RIGHT_CLOSE.height)
            if (pipeClosedEnds[2])
                Renderer.renderTexture(Image.Block.TUBE_DOWN_CLOSE, x, y + 14 - Image.Block.TUBE_DOWN_CLOSE.height)
            if (pipeClosedEnds[3])
                Renderer.renderTexture(Image.Block.TUBE_LEFT_CLOSE, x - Image.Block.TUBE_LEFT_CLOSE.width, y + 20 - Image.Block.TUBE_LEFT_CLOSE.height)
        } else {
            super.render()
        }
    }

    override fun toReference() = GhostLevelObjectReference(this)
}