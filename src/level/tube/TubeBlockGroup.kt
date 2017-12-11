package level.tube

import graphics.Image
import graphics.Renderer
import inv.Item
import inv.ItemType
import level.node.InputNode
import level.node.OutputNode
import level.node.StorageNode
import level.node.TransferNode
import level.resource.ResourceType
import misc.ConcurrentlyModifiableMutableList
import java.io.DataOutputStream

data class ItemPackage(val item: Item, val goal: OutputNode<ItemType>, var xPixel: Int, var yPixel: Int, var dir: Int = 0)

data class IntersectionTube(val tubeBlock: TubeBlock, val connectedTo: Array<IntersectionTube?>)

class TubeBlockGroup {

    val tubes = object : ConcurrentlyModifiableMutableList<TubeBlock>() {
        override fun add(l: TubeBlock) {
            super.add(l)
            if (!combining)
                createCorrespondingNodes(l)
        }

        override fun remove(l: TubeBlock) {
            super.remove(l)
            if (!combining)
                removeCorrespondingNodes(l)
        }
    }

    private var inputs = mutableListOf<InputNode<ItemType>>()
    private var outputs = mutableListOf<OutputNode<ItemType>>()
    private val intersections = mutableListOf<IntersectionTube>()
    private val storage = TubeBlockGroupInternalStorage(this)
    var ticksSinceLastOutput = 0
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

    // Creates transfer nodes corresponding the inputted nodes
    fun createCorrespondingNodes(nodes: List<TransferNode<*>>) {
        for (n in nodes) {
            if (n is InputNode) {
                // Ok because tubes only connect to item nodes
                outputs.add(OutputNode.createCorrespondingNode(n as InputNode<ItemType>, storage))
            } else if (n is OutputNode) {
                inputs.add(InputNode.createCorrespondingNode(n as OutputNode<ItemType>, storage))
            }
        }
    }

    // Creates transfer nodes corresponding to the tube's nodes
    fun createCorrespondingNodes(tubeBlock: TubeBlock) {
        tubeBlock.nodeConnections.forEach { createCorrespondingNodes(it) }
    }

    // Removes all nodes that were given to the network because of this
    fun removeCorrespondingNodes(t: TubeBlock) {
        inputs = inputs.filter { it.xTile != t.xTile && it.yTile != t.yTile }.toMutableList()
        outputs = outputs.filter { it.xTile != t.xTile && it.yTile != t.yTile }.toMutableList()
    }

    // If this tube is a valid intersection, it will recursively go down, find all other intersections and connect them back to this.
    // This should only be called on the first intersection of the network
    fun convertToIntersection(tube: TubeBlock): IntersectionTube? {
        if (tube.texture != Image.Block.TUBE_2_WAY_HORIZONTAL && tube.texture != Image.Block.TUBE_2_WAY_VERTICAL) {
            return IntersectionTube(tube, findIntersections(tube))
        }
        return null
    }

    fun findIntersections(tube: TubeBlock): Array<IntersectionTube?> {
        // If it is not a vertical or horizontal pipe, it must be an intersection
        val arr = arrayOfNulls<IntersectionTube>(4)
        for (i in 0..3) {
            var currentTube = tube
            while (currentTube.tubeConnections[i] != null)
                currentTube = currentTube.tubeConnections[i]!!
            if (currentTube != tube)
                arr[i] = convertToIntersection(currentTube)
        }
        return arr
    }

    //fun route(input: InputNode<*>, output: OutputNode<*>): List<IntersectionTube> {
    //    return
    //}

    val size
        get() = tubes.size

    fun update() {
        ticksSinceLastOutput++
    }

    fun save(out: DataOutputStream) {

    }

    fun renderDebug() {
        tubes.forEach {
            Renderer.renderText(ALL.indexOf(this), it.xPixel + 8, it.yPixel + 8)
        }

    }

    companion object {

        const val DEFAULT_TICKS_PER_STACK = 30
        const val STACK_TRANSFER_SPEED = 30

        val ALL = mutableListOf<TubeBlockGroup>()

        fun update() {
            ALL.forEach { it.update() }
        }
    }

    // When an output node tries to input into this, do nothing if there is nowhere to put it
    // Each stack inputted is stored as separate, even if they could be combined. If there is more than 1 stack inputted at a time,
    // split it up and send it with a delay
    class TubeBlockGroupInternalStorage(val parent: TubeBlockGroup) : StorageNode<ItemType>(ResourceType.ITEM) {

        var stackCount = 0

        val maxStacks
            get() = parent.tubes.size

        var itemCount = 0

        val itemsBeingMoved = mutableListOf<ItemPackage>()

        override fun add(resource: ItemType, quantity: Int, checkForSpace: Boolean): Boolean {
            if (checkForSpace)
                if (!spaceFor(resource, quantity))
                    return false
            // As always, assume that we know it can be sent
            itemCount += quantity
            stackCount += Math.ceil(quantity.toDouble() / resource.maxStack).toInt()
            return false
        }

        override fun remove(resource: ItemType, quantity: Int, checkIfContains: Boolean): Boolean {
            if (checkIfContains)
                if (!contains(resource, quantity))
                    return false

            return false
        }

        override fun spaceFor(resource: ItemType, quantity: Int): Boolean {
            return parent.outputs.any { it.canOutput(resource, quantity) }
        }

        override fun contains(resource: ItemType, quantity: Int): Boolean {
            return false
        }

    }

}