package inv

import graphics.Images
import graphics.Texture
import level.block.BlockType
import level.block.BlockTypes

object ItemTypes {
    val ALL = mutableListOf<ItemType>()
    val ERROR = ItemType("Error", Images.ERROR, maxStack = 5)
    val TEST = ItemType("Test", Images.ERROR, maxStack = 5)
}

private var nextID = 0

class ItemType(val name: String, val texture: Texture, private val placedBlockID: Int = 1, val maxStack: Int) {

    val placedBlock: BlockType
        get() = BlockTypes.getByID(placedBlockID)!!

    val id = nextID++

    init {
        ItemTypes.ALL.add(this)
    }

    override fun toString() = name

    override fun equals(other: Any?): Boolean {
        return other is ItemType && other.id == id
    }
}