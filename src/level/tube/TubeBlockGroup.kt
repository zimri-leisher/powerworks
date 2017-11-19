package level.tube

import inv.ItemType
import level.node.InputNode
import level.node.OutputNode

class TubeBlockGroup {
    val tubes = mutableListOf<TubeBlock>()
    val inputs = mutableListOf<InputNode<ItemType>>()
    val outputs = mutableListOf<OutputNode<ItemType>>()
}