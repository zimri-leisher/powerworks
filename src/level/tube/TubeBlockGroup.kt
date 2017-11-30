package level.tube

import graphics.Renderer
import inv.Inventory
import inv.Item
import inv.ItemType
import level.node.InputNode
import level.node.OutputNode
import level.node.TransferNode
import misc.ConcurrentlyModifiableMutableList
import java.io.DataOutputStream

data class ItemPackage(val item: Item, val goal: OutputNode<ItemType>, var pos: Int = 0)

class TubeBlockGroup {
    private val tubes = ConcurrentlyModifiableMutableList<TubeBlock>()
    private var inputs = mutableListOf<InputNode<ItemType>>()
    private var outputs = mutableListOf<OutputNode<ItemType>>()
    private val inv = Inventory(10, 10)
    /**
     * This is for optimization: if it is true, we will not create connections when a new tube is added because the process
     * can be simplified to simply combining the lists of ins and outs
     */
    private var combining = false

    init {
        ALL.add(this)
    }

    /**
     * Combines the two and sets all the appropriate values for their tubes, inputs and outputs
     */
    fun combine(other: TubeBlockGroup) {
        // so that they don't bother making or removing connections
        combining = true
        other.tubes.forEach {
            it.group = this
        }
        combining = false
        inputs.addAll(other.inputs)
        outputs.addAll(other.outputs)
    }

    // Called by TubeBlock.setGroup
    fun addTube(tubeBlock: TubeBlock) {
        tubes.add(tubeBlock)
        if (!combining)
            connect(tubeBlock)
    }

    // Called by TubeBlock.setGroup
    fun removeTube(tubeBlock: TubeBlock) {
        tubes.remove(tubeBlock)
        if (!combining)
            disconnect(tubeBlock)
    }

    fun connect(nodes: List<TransferNode<*>>) {
        for (n in nodes) {
            if (n is InputNode) {
                // Ok because tubes only connect to item nodes
                outputs.add(OutputNode.createCorrespondingNode<ItemType>(n as InputNode<ItemType>, inv))
            } else if (n is OutputNode) {
                inputs.add(InputNode.createCorrespondingNode<ItemType>(n as OutputNode<ItemType>, inv))
            }
            println("tube group connecting with $n")
        }
    }

    fun connect(tubeBlock: TubeBlock) {
        tubeBlock.nodeConnections.forEach { connect(it) }
    }

    fun disconnect(t: TubeBlock) {
        inputs = inputs.filter { it.xTile != t.xTile && it.yTile != t.yTile }.toMutableList()
        outputs = outputs.filter { it.xTile != t.xTile && it.yTile != t.yTile }.toMutableList()
        println("tube group disconnecting $t")
    }

    val size
        get() = tubes.size

    fun update() {
        var c = 0
        while (inv.itemCount > 0 && c++ < 30) {
            // If the inventory is properly sorted we can be sure that if there is at least one item there is at least one
            // item in the first slot
            val i = inv[0]!!
            if (outputs.isEmpty())
                break
            for (o in outputs) {
                if (o.output(i.type, i.quantity))
                    break
            }
        }
    }

    fun save(out: DataOutputStream) {

    }

    fun renderDebug() {
        tubes.forEach {
            Renderer.renderText(ALL.indexOf(this), it.xPixel + 8, it.yPixel + 8)
        }
    }

    companion object {
        val ALL = mutableListOf<TubeBlockGroup>()

        fun update() {
            ALL.forEach { it.update() }
        }
    }

    /*
    class TubeBlockGroupInternalStorage : StorageNode<ItemType>(ResourceType.ITEM) {

        val itemsBeingMoved = mutableListOf<ItemPackage>()

        override fun add(resource: ItemType, quantity: Int): Boolean {

        }

        override fun spaceFor(resource: ItemType, quantity: Int): Boolean {
        }

        override fun remove(resource: ItemType, quantity: Int): Boolean {
        }

        override fun contains(resource: ItemType, quantity: Int): Boolean {
        }

    }
    */
}