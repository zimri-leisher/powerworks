package level.tube

import inv.Item
import inv.ItemType
import level.node.InputNode
import level.node.OutputNode
import level.node.StorageNode
import level.node.TransferNode
import level.resource.ResourceType
import misc.GeometryHelper
import misc.WeakMutableList
import java.io.DataOutputStream
import java.util.*

data class ItemPackage(val item: Item, val goal: OutputNode<ItemType>, var currentIndex: Int, val path: Map<IntersectionTube, Int>, var xPixel: Int, var yPixel: Int, var dir: Int = 0)

data class IntersectionTube(val tubeBlock: TubeBlock, var connectedTo: Array<TubeAndDist?>) {
    override fun equals(other: Any?): Boolean {
        return other is IntersectionTube && other.tubeBlock == tubeBlock
    }

    override fun hashCode(): Int {
        return tubeBlock.hashCode()
    }

    override fun toString(): String {
        return "Intersection at ${tubeBlock.xTile}, ${tubeBlock.yTile}"
    }
}

data class TubeAndDist(val intersection: IntersectionTube, val dist: Int)

class TubeBlockGroup {

    private val tubes = mutableListOf<TubeBlock>()

    private var inputs = mutableListOf<InputNode<ItemType>>()
    private var outputs = mutableListOf<OutputNode<ItemType>>()
    val intersections = mutableListOf<IntersectionTube>()
    private val storage = TubeBlockGroupInternalStorage(this)
    var ticksSinceLastOutput = 0
    val id = nextId++

    init {
        ALL.add(this)
    }

    /**
     * Combines the two and sets all the appropriate values for their tubes, inputs and outputs
     */
    fun combine(other: TubeBlockGroup) {
        // so that they don't bother making or removing connections, because it would just remove a connection and add it again
        other.tubes.forEach {
            it.group = this@TubeBlockGroup
            if (it !in tubes)
                tubes.add(it)
        }
        other.inputs.forEach {
            if (it !in inputs)
                inputs.add(it)
        }
        other.outputs.forEach {
            if (it !in outputs)
                outputs.add(it)
        }
    }

    fun addTube(t: TubeBlock) {
        createCorrespondingNodes(t)
        tubes.add(t)
    }

    fun removeTube(t: TubeBlock) {
        removeCorrespondingNodes(t)
        tubes.remove(t)
    }

    /**
     * Creates transfer nodes corresponding the inputted nodes
     */
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

    /**
     * Creates transfer nodes corresponding to the tube's nodes
     */
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
        if (isIntersection(tube)) {
            println("tube is an intersection ${tube.xTile}, ${tube.yTile}")
            return getIntersection(tube)
        }
        return null
    }

    private fun getIntersection(tube: TubeBlock): IntersectionTube {
        val i = intersections.firstOrNull { it.tubeBlock == tube }
        if (i != null) {
            println("already found")
            return i
        } else {
            println("creating")
            val t = IntersectionTube(tube, arrayOfNulls(4))
            intersections.add(t)
            t.connectedTo = findIntersections(t)
            return t
        }
    }

    fun findIntersections(tube: IntersectionTube): Array<TubeAndDist?> {
        val arr = arrayOfNulls<TubeAndDist>(4)
        for (i in 0..3) {
            var currentTube = tube.tubeBlock.tubeConnections[i]
            var dist = 1
            // Iterate down the tube until there are no more connections
            while (currentTube != null && !isIntersection(currentTube)) {
                currentTube = currentTube.tubeConnections[i]
                dist++
            }
            if (currentTube != tube.tubeBlock && currentTube != null) {
                arr[i] = TubeAndDist(convertToIntersection(currentTube)!!.apply {
                    connectedTo[GeometryHelper.getOppositeAngle(i)] = TubeAndDist(tube, dist)
                }, dist)
            }
        }
        return arr
    }

    fun isIntersection(t: TubeBlock): Boolean {
        return t.state in TubeState.Group.INTERSECTION || t.nodeConnections.any { it.isNotEmpty() }
    }

    class Node(val parent: Node? = null, val goal: IntersectionTube, val intersection: IntersectionTube, val directionFromParent: Int, val g: Int, val h: Int) {

        val xTile: Int
            get() = intersection.tubeBlock.xTile
        val yTile: Int
            get() = intersection.tubeBlock.yTile
        val f = h + g

        fun getChildren(): List<Node> {
            val l = mutableListOf<Node>()
            for (i in 0 until 4) {
                // if the intersection at this node has a connection in this dir
                if (intersection.connectedTo[i] != null) {
                    val newIntersection = intersection.connectedTo[i]!!.intersection
                    val dist = intersection.connectedTo[i]!!.dist
                    l.add(Node(this, goal, newIntersection,
                            i,
                            g + dist,
                            Math.abs(goal.tubeBlock.xTile - newIntersection.tubeBlock.xTile) + Math.abs(goal.tubeBlock.yTile - newIntersection.tubeBlock.yTile)))
                }
            }
            return l
        }

        override fun equals(other: Any?): Boolean {
            return other is Node && other.xTile == xTile && other.yTile == yTile && other.g == g && other.h == h && other.directionFromParent == directionFromParent
        }

        override fun toString(): String {
            return "$xTile, $yTile, dir from parent: $directionFromParent"
        }
    }

    private data class IntersectionAndDir(val intersection: IntersectionTube, var dir: Int)

    fun route(input: InputNode<*>, output: OutputNode<*>): Map<IntersectionTube, Int>? {
        val inp = findCorrespondingIntersection(input)!!
        val out = findCorrespondingIntersection(output)!!
        val startNode = Node(null, out, inp, -1, 0, 0)

        val possibleNextNodes = mutableListOf<Node>()
        val alreadyUsedNodes = mutableListOf<Node>()
        possibleNextNodes.add(startNode)

        var finalNode: Node? = null

        main@ while (possibleNextNodes.isNotEmpty()) {
            // next can't be null because it's not empty
            val nextNode = possibleNextNodes.minBy { it.f }!!

            possibleNextNodes.remove(nextNode)

            val nodeChildren = nextNode.getChildren()
            for (nodeChild in nodeChildren) {
                if (nodeChild.xTile == out.tubeBlock.xTile && nodeChild.yTile == out.tubeBlock.yTile) {
                    finalNode = nodeChild
                    break@main
                }

                // there's a better path to here
                if (possibleNextNodes.any { it.xTile == nodeChild.xTile && it.yTile == nodeChild.yTile && it.f < nodeChild.f }) {
                    continue
                }
                // this node has already been moved to in a better path
                if (alreadyUsedNodes.any { it.xTile == nodeChild.xTile && it.yTile == nodeChild.yTile && it.f < nodeChild.f }) {
                    continue
                }
                possibleNextNodes.add(nodeChild)
            }
            alreadyUsedNodes.add(nextNode)
        }
        if (finalNode != null) {
            val instructions = mutableListOf<IntersectionAndDir>()
            instructions.add(IntersectionAndDir(finalNode.intersection, -1))
            val endNode = finalNode
            while(finalNode!!.parent != null) {
                instructions.add(IntersectionAndDir(finalNode.parent!!.intersection, finalNode.directionFromParent))
                finalNode = finalNode.parent
            }
            Collections.reverse(instructions)
            println(instructions.joinToString("\n"))
        }
        return null
    }

    fun findCorrespondingIntersection(t: TransferNode<*>): IntersectionTube? {
        return intersections.firstOrNull { it.tubeBlock.xTile == t.xTile && it.tubeBlock.yTile == t.yTile }
    }

    val size
        get() = tubes.size

    fun update() {
        ticksSinceLastOutput++
    }

    fun save(out: DataOutputStream) {

    }

    companion object {

        const val DEFAULT_TICKS_PER_STACK = 30
        const val STACK_TRANSFER_SPEED = 30

        var nextId = 0

        val ALL = WeakMutableList<TubeBlockGroup>()

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

        override fun add(resource: ItemType, quantity: Int, input: InputNode<ItemType>?, checkForSpace: Boolean): Boolean {
            if (input != null) {
                val output = parent.outputs.firstOrNull { it.canOutput(resource, quantity) }
                if (output != null) {
                    val t = parent.route(input, output)
                    if (t != null) {
                    }
                }
            }
            return false
        }

        override fun remove(resource: ItemType, quantity: Int, checkIfContains: Boolean): Boolean {
            if (checkIfContains)
                if (!contains(resource, quantity))
                    return false
            return false
        }

        override fun spaceFor(resource: ItemType, quantity: Int): Boolean {
            return true
        }

        override fun contains(resource: ItemType, quantity: Int): Boolean {
            return true
        }

        override fun clear() {
            itemsBeingMoved.clear()
            itemCount = 0
            stackCount = 0
        }

    }
    // when a tube block gets placed, it checks for nearby networks and merges them as appropriate.
    // if there is a block with no network next to it, they create a new one
    // if there is a block with a network next to it, it joins
    // if it has a network and there is a block with a network the smallest network merges into the largest
    // TODO optimize algorithim for tube group merging so that if there are multiple groups (more than 2) that have to join tthey both just join the largest
    // on add a block to network:
    //  if the block is an intersection
    //   find the next intersection in each direction it is connected on, set both of them to each other
    //   add it to the intersections list
    // on a block in the network change connection:
    //  if the block was not an intersection and is now:
    //   same logic as adding an intersection
    //  else if the block was an intersection and is not:
    //   find the next intersection in each direction and remove its connection to this, if it only has 1 intersection connection now
    // on remove a block from the network:
    //

}