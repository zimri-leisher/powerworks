package level.block

import graphics.Renderer
import io.*
import level.CHUNK_TILE_EXP
import level.Hitbox
import level.Level
import level.LevelObject
import main.Game
import resource.ResourceNodeGroup
import screen.Mouse
import java.io.DataOutputStream

abstract class Block(type: BlockTemplate<out Block>, xTile: Int, yTile: Int, var rotation: Int = 0, hitbox: Hitbox = type.hitbox, requiresUpdate: Boolean = type.requiresUpdate) : LevelObject(xTile shl 4, yTile shl 4, hitbox, requiresUpdate) {

    open val type = type

    val nodes = ResourceNodeGroup("$this node group", type.nodesTemplate.instantiate(xTile, yTile, rotation))

    /**
     * Don't forget to call super.onAddToLevel() so that the onAdjacentBlockAdd methods of adjacent blocks are called
     */
    override fun onAddToLevel() {
        nodes.forEach { Level.add(it) }
        for (y in -1..1) {
            for (x in -1..1) {
                if (Math.abs(x) != Math.abs(y)) {
                    Level.Blocks.get(xTile + x, yTile + y)?.onAdjacentBlockAdd(this)
                }
            }
        }
    }

    /**
     * Don't forget to call super.onRemoveFromLevel() so that the onAdjacentBlockRemove methods of adjacent blocks are called
     */
    override fun onRemoveFromLevel() {
        nodes.forEach { Level.remove(it) }
        for (y in -1..1) {
            for (x in -1..1) {
                if (Math.abs(x) != Math.abs(y))
                    Level.Blocks.get(xTile + x, yTile + y)?.onAdjacentBlockRemove(this)
            }
        }
    }

    override fun render() {
        val texture = type.textures[rotation]
        Renderer.renderTexture(texture.texture, xPixel - texture.xPixelOffset, yPixel - texture.yPixelOffset)
        super.render()
    }

    /**
     * When an adjacent block is removed
     */
    open fun onAdjacentBlockRemove(b: Block) {

    }

    /**
     * When an adjacent block is added
     */
    open fun onAdjacentBlockAdd(b: Block) {

    }

    override fun getCollision(xPixel: Int, yPixel: Int, predicate: ((LevelObject) -> Boolean)?): LevelObject? {
        // Check if a block is already present
        val nXTile = xPixel shr 4
        val nYTile = yPixel shr 4
        for (x in nXTile until (nXTile + type.widthTiles)) {
            for (y in nYTile until (nYTile + type.heightTiles)) {
                val c = Level.Chunks.get(x shr CHUNK_TILE_EXP, y shr CHUNK_TILE_EXP)
                val b = c.getBlock(x, y)
                if (b != null) {
                    if (predicate != null && !predicate(b)) {
                        continue
                    }
                    // check memory because we could be trying to put the same block down at the same place
                    if (b !== this) {
                        return b
                    }
                }
            }
        }
        // Checks for moving objects. Don't worry about blocks, because no block has a hitbox of over a tile
        return Level.MovingObjects.getCollision(this, xPixel, yPixel, predicate)
    }

    override fun save(out: DataOutputStream) {
        super.save(out)
        out.writeInt(rotation)
        out.writeInt(type.id)
    }

    override fun toString(): String {
        return "Block at $xTile, $yTile, type: $type"
    }

    override fun equals(other: Any?): Boolean {
        return other is Block && other.xTile == xTile && other.yTile == yTile && other.type == type
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + rotation
        result = 31 * result + xTile
        result = 31 * result + yTile
        return result
    }

    companion object : ControlPressHandler {

        init {
            InputManager.registerControlPressHandler(this, ControlPressHandlerType.LEVEL_ANY, Control.INTERACT, Control.SECONDARY_INTERACT)
        }

        override fun handleControlPress(p: ControlPress) {
            if(p.pressType == PressType.PRESSED) {
                val block = Game.currentLevel.selectedLevelObject
                if(block is Block) {
                    if(p.control == Control.SECONDARY_INTERACT) {
                        Level.remove(block)
                        // TODO
                    }
                } else if(block == null) {
                    if(p.control == Control.INTERACT && Game.currentLevel.ghostBlock != null) {
                        val gBlock = Game.currentLevel.ghostBlock!!
                        Level.add(gBlock.type.instantiate(gBlock.xTile, gBlock.yTile, gBlock.rotation))
                        val h = Mouse.heldItemType!!
                        Game.mainInv.remove(h, 1)
                    }
                }
            }
        }
    }
}