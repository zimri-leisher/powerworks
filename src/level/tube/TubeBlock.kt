package level.tube

import graphics.Renderer
import level.block.Block
import level.block.BlockType
import main.Game
import misc.GeometryHelper.getXSign
import misc.GeometryHelper.getYSign

class TubeBlock(xTile: Int, yTile: Int) : Block(xTile, yTile, BlockType.TUBE) {

    var texture = type.getTexture(rotation)
    var up: TubeBlock? = null
    var down: TubeBlock? = null
    var left: TubeBlock? = null
    var right: TubeBlock? = null

    override fun onAddToLevel() {
        updateConnections()
    }

    override fun onRemoveFromLevel() {

    }

    fun updateConnection(dir: Int) {
        when(dir) {
            0 -> {
                up = getConnectableTubeAt(0)
                if(up != null) up!!.down = this
            }
            1 -> {
                right = getConnectableTubeAt(1)
                if(right != null) right!!.left = this
            }
            2 -> {
                down = getConnectableTubeAt(2)
                if(down != null) down!!.up = this
            }
            3 -> {
                left = getConnectableTubeAt(3)
                if(left != null) left!!.right = this
            }
        }
    }

    fun updateTexture() {
        if(up != null) {
            if(down != null) {
                if(left != null) {
                    if(right != null) {
                    }
                }
            }
        }
    }

    fun updateConnections() {
        for(i in 0..3)
            updateConnection(i)
    }

    fun getConnectableTubeAt(dir: Int): TubeBlock? {
        val b = Game.currentLevel.getBlock(xTile + 16 * getXSign(dir), yTile + 16 * getYSign(dir))
        if (b != null && b is TubeBlock) {
            return b
        }
        return null
    }

    override fun render() {
        Renderer.renderTexture(texture, xPixel - type.textureXPixelOffset, yPixel - type.textureYPixelOffset)
        super.render()
    }

}