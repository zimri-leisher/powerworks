package behavior.leaves

import behavior.BehaviorTree
import behavior.Composite
import behavior.DataLeaf
import level.entity.Entity
import behavior.Node

class Reset(parent: BehaviorTree, val forNode: Node) : DataLeaf(parent) {
    override fun run(entity: Entity): Boolean {

        fun _recursivelyDeleteData(forNode: Node) {
            parent.data.deleteCorresponding(forNode)
            if(forNode is Composite) {
                forNode.children.forEach { _recursivelyDeleteData(it) }
            }
        }

        _recursivelyDeleteData(forNode)
        return true
    }
}