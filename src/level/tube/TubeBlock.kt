package level.tube

import graphics.Image
import graphics.ImageCollection
import graphics.Renderer
import graphics.Texture
import level.block.Block
import level.block.BlockType
import level.node.TransferNode
import main.Game
import misc.GeometryHelper
import misc.GeometryHelper.getOppositeAngle
import misc.GeometryHelper.getXSign
import misc.GeometryHelper.getYSign
import misc.GeometryHelper.isOppositeAngle

class TubeBlock(xTile: Int, yTile: Int) : Block(xTile, yTile, BlockType.TUBE) {

    var texture = type.getTexture(rotation)

    val tubeConnections = arrayOfNulls<TubeBlock>(4)
    val nodeConnections = arrayOf<MutableList<TransferNode<*>>>(mutableListOf(), mutableListOf(), mutableListOf(), mutableListOf())

    val closedDirections = arrayOf(false, false, false, false)

    var group: TubeBlockGroup? = null
        set(value) {
            if (field != value) {
                if (field != null) {
                    field!!.removeTube(this)
                }
                field = value
                if (value != null) {
                    value.addTube(this)
                }
            }
        }

    override fun onAddToLevel() {
        updateConnections()
        updateTexture()
        super.onAddToLevel()
    }

    override fun onRemoveFromLevel() {
        for (i in 0..3) {
            tubeConnections[i]?.updateTubeConnection(getOppositeAngle(i))
            tubeConnections[i]?.updateTexture()
        }
        group = null
        super.onRemoveFromLevel()
    }

    override fun onAdjacentBlockAdd(b: Block) {
        // If it is a tube block, its onAddToLevel() event will call updateConnections() which will do the connecting for us,
        // so no need to worry about us doing it for them
        if (b !is TubeBlock) {
            updateNodeConnections(GeometryHelper.getDir(b.xTile - xTile, b.yTile - yTile))
            updateTexture()
        }
    }

    override fun onAdjacentBlockRemove(b: Block) {
        // This is an optimization to skip the uncessecary getting of nodes when calling updateNodeConnection(dir) - we know there aren't any because there can't be any blocks there,
        // and we assume no blocks = no nodes
        if (b !is TubeBlock) {
            nodeConnections[GeometryHelper.getDir(b.xTile - xTile, b.yTile - yTile)].clear()
            updateTexture()
        }
    }

    fun updateTubeConnection(dir: Int) {
        // If there is a node connection, and tubes can't have nodes connecting to other tubes, then no need to check
        if (nodeConnections[dir].isEmpty()) {
            val new = getTubeAt(dir)
            // Don't do anything if there was no change
            if (tubeConnections[dir] != new) {
                tubeConnections[dir] = getTubeAt(dir)
                if (new != null) {
                    println("connected with tube")
                    mergeGroups(new)
                    new.tubeConnections[getOppositeAngle(dir)] = this
                    new.updateTexture()
                }
            }
        }
    }

    /**
     * Combine the groups of this and another tube block.
     * If neither have a group, a new one is made.
     * If only one has a group, the other one gets that one
     * If both have a group, the pre-existing group with the lowest tube count gets merged into the other
     */
    private fun mergeGroups(t: TubeBlock) {
        if (t.group == null) {
            // Neither
            if (group == null) {
                group = TubeBlockGroup()
                t.group = this.group
                // Only 1
            } else {
                t.group = group
            }
        } else {
            // Only 1
            if (group == null)
                group = t.group
            // Both
            else {
                if (group!!.size > t.group!!.size)
                    group!!.combine(t.group!!)
                else
                    t.group!!.combine(group!!)
            }
        }
    }

    fun updateNodeConnections(dir: Int) {
        // If there is a node connection, and tubes can't have nodes connecting to other tubes, then no need to check
        if (tubeConnections[dir] == null) {
            // Get all nodes that could possibly disconnect to a node if placed here
            val nodes = Game.currentLevel.getAllTransferNodes(xTile + getXSign(dir), yTile + getYSign(dir), { isOppositeAngle(it.dir, dir) })
            nodeConnections[dir] = nodes
            if (nodes.isNotEmpty()) {
                println("connected with node")
                if (group != null)
                    group!!.connect(nodes)
            }
        }
    }

    fun updateConnections() {
        for (i in 0..3)
            updateConnection(i)
    }

    fun updateConnection(dir: Int) {
        updateTubeConnection(dir)
        updateNodeConnections(dir)
    }

    fun updateTexture() {
        val dirs = mutableListOf<Int>()
        for (i in 0..3)
        // If it is connected to a tube or at least one node at that dir, add it
            if (tubeConnections[i] != null || nodeConnections[i].isNotEmpty())
                dirs.add(i)
        dirs.sort()
        texture = getTexture(dirs)
    }

    private fun getTexture(dirs: List<Int>): Texture {
        for (i in closedDirections.indices)
            closedDirections[i] = false
        if (dirs.size == 4) {
            return Image.Block.TUBE_4_WAY
        }
        // T shaped pipes
        if (dirs.size == 3) {
            return ImageCollection.TUBE_3_WAY[getOppositeAngle(getMissingDir(dirs))]
        }
        if (dirs.size == 2) {
            // If it is a vertical or horizontal connection
            if (dirs[0] == getOppositeAngle(dirs[1])) {
                return if (dirs.contains(0)) Image.Block.TUBE_2_WAY_VERTICAL else Image.Block.TUBE_2_WAY_HORIZONTAL
            }
            // Corner connections
            if (dirs[0] == 0) {
                // Up to right
                if (dirs[1] == 1)
                    return ImageCollection.TUBE_CORNER[0]
                // Up to left
                else
                    return ImageCollection.TUBE_CORNER[3]
            }
            // I figured this out. just some optimizations/less logic
            return ImageCollection.TUBE_CORNER[dirs[0]]
        }
        if (dirs.size == 1) {
            // If it starts out with a vertical direction
            if (dirs[0] % 2 == 0) {
                closedDirections[getOppositeAngle(dirs[0])] = true
                return Image.Block.TUBE_2_WAY_VERTICAL
            } else {
                closedDirections[getOppositeAngle(dirs[0])] = true
                return Image.Block.TUBE_2_WAY_HORIZONTAL
            }
        }
        closedDirections[0] = true
        closedDirections[2] = true
        for (i in closedDirections.indices) {
            // Stop it from closing off ends that are connected to nodes
            closedDirections[i] = closedDirections[i] && nodeConnections[i].isEmpty()
        }
        return Image.Block.TUBE_2_WAY_VERTICAL
    }

    /**
     * Returns the direction that is missing from the list
     */
    private fun getMissingDir(dirs: List<Int>): Int {
        for (i in 0..3)
            if (!dirs.contains(i))
                return i
        return -1
    }

    fun getTubeAt(dir: Int): TubeBlock? {
        val b = Game.currentLevel.getBlock(xTile + getXSign(dir), yTile + getYSign(dir))
        if (b != null && b is TubeBlock) {
            return b
        }
        return null
    }

    override fun render() {
        Renderer.renderTexture(texture, xPixel - type.textureXPixelOffset, yPixel - type.textureYPixelOffset)
        if (closedDirections[0])
            Renderer.renderTexture(Image.Block.TUBE_UP_CLOSE, xPixel, yPixel - Image.Block.TUBE_UP_CLOSE.heightPixels + 4)
        if (closedDirections[1])
            Renderer.renderTexture(Image.Block.TUBE_RIGHT_CLOSE, xPixel + 16, yPixel)
        if (closedDirections[2])
            Renderer.renderTexture(Image.Block.TUBE_DOWN_CLOSE, xPixel, yPixel + 8)
        if (closedDirections[3])
            Renderer.renderTexture(Image.Block.TUBE_LEFT_CLOSE, xPixel - Image.Block.TUBE_LEFT_CLOSE.widthPixels, yPixel)
        if (Game.RENDER_HITBOXES)
            renderHitbox()
    }
}