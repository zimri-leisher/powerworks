package inv

import graphics.Image
import graphics.Texture
import level.block.BlockType

private var nextID = 0

sealed class ItemType(val name: String, val texture: Texture, private val placedBlockID: Int = 1, val maxStack: Int) {

    object ERROR : ItemType("Error", Image.ERROR, maxStack = 5)

    object TEST : ItemType("Test", Image.ERROR, BlockType.ERROR.id, 5)

    val placedBlock: BlockType
        get() = BlockType.getByID(placedBlockID)!!

    val id = nextID++

    init {
        ALL.add(this)
    }

    override fun toString() = name

    override fun equals(other: Any?): Boolean {
        return other is ItemType && other.id == id
    }

    override fun hashCode(): Int {
        return id
    }

    companion object {
        val ALL = mutableListOf<ItemType>()
    }
}

sealed class WeaponItemType(name: String, texture: Texture, placedBlockID: Int, maxStack: Int) : ItemType(name, texture, placedBlockID, maxStack) {

}