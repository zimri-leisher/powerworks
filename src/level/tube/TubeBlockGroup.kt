package level.tube

import graphics.Renderer
import inv.Item
import inv.ItemType
import level.Level
import level.Level.ResourceNodes.addTransferNode
import level.node.*
import level.resource.ResourceType
import main.Game
import misc.GeometryHelper
import misc.PixelCoord
import misc.WeakMutableList
import java.io.DataOutputStream
import java.util.*

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
    val intersections = mutableListOf<IntersectionTube>()
    private val storage = TubeBlockGroupInternalStorage(this)
    val id = nextId++
    private val transferNodes = NodeGroup("Tube block group $id")

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
        transferNodes.addAll(other.transferNodes)
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
    fun createCorrespondingNodes(nodes: List<ResourceTransferNode<*>>) {
        for (n in nodes) {
            if (n is InputNode<*>) {
                // Ok because tubes only connect to item nodes
                val a = OutputNode.createCorrespondingNode(n as InputNode<ItemType>, storage)
                transferNodes.add(a)
                Level.ResourceNodes.addTransferNode(a)
            } else if (n is OutputNode<*>) {
                val a = InputNode.createCorrespondingNode(n as OutputNode<ItemType>, storage)
                transferNodes.add(a)
                Level.ResourceNodes.addTransferNode(a)
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
        transferNodes.removeAll{ it is OutputNode<*> || it is InputNode<*> && it.xTile != t.xTile && it.yTile != t.yTile}
    }

    fun convertToIntersection(tube: TubeBlock): IntersectionTube? {
        if (isIntersection(tube)) {
            return getIntersection(tube)
        }
        return null
    }

    private fun getIntersection(tube: TubeBlock): IntersectionTube {
        val i = intersections.firstOrNull { it.tubeBlock == tube }

        if (i != null) {
            return i
        } else {
            val t = IntersectionTube(tube, arrayOfNulls(4))
            intersections.add(t)
            t.connectedTo = findIntersections(t)
            return t
        }
    }

    private fun findIntersections(tube: IntersectionTube): Array<TubeAndDist?> {
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
            } else {
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

    private fun route(input: InputNode<*>, output: OutputNode<*>): ItemPath? {
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
            while (finalNode!!.parent != null) {
                instructions.add(IntersectionAndDir(finalNode.parent!!.intersection, finalNode.directionFromParent))
                finalNode = finalNode.parent
            }
            Collections.reverse(instructions)
            return ItemPath(instructions)
        }
        return null
    }

    private fun findCorrespondingIntersection(t: ResourceTransferNode<*>): IntersectionTube? {
        return intersections.firstOrNull { it.tubeBlock.xTile == t.xTile && it.tubeBlock.yTile == t.yTile }
    }

    val size
        get() = tubes.size

    fun update() {
        storage.update()
    }

    fun render() {
        storage.render()
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

        fun render() {
            ALL.forEach { it.render() }
        }
    }

    private data class ItemPackage(val item: Item, val goal: OutputNode<ItemType>, var currentIndex: Int, val path: ItemPath, var xPixel: Int, var yPixel: Int, var dir: Int = 0)

    // When an output node tries to input into this, do nothing if there is nowhere to put it
    // Each stack inputted is stored as separate, even if they could be combined. If there is more than 1 stack inputted at a time,
    // split it up and send it with a delay
    class TubeBlockGroupInternalStorage(val parent: TubeBlockGroup) : StorageNode<ItemType>(ResourceType.ITEM) {

        private val itemsBeingMoved = mutableListOf<ItemPackage>()

        override fun add(resource: ItemType, quantity: Int, from: InputNode<ItemType>?, checkForSpace: Boolean): Boolean {
            if (from != null) {
                // we assume tubes will only carry items
                val output = parent.transferNodes.firstOrNull { it is OutputNode<*> && (it as OutputNode<ItemType>).canOutput(resource, quantity) }
                if (output != null) {
                    val t = parent.route(from, output)
                    if (t != null) {
                        itemsBeingMoved.add(ItemPackage(Item(resource, quantity), output, 0, t, from.xTile shl 4, from.yTile shl 4, GeometryHelper.getOppositeAngle(from.dir)))
                        return true
                    }
                }
            }
            return false
        }

        fun update() {
            val iterator = itemsBeingMoved.iterator()
            for (item in iterator) {
                if (item.xPixel == item.path[item.currentIndex].coord.xPixel && item.yPixel == item.path[item.currentIndex].coord.yPixel) {
                    item.dir = item.path[item.currentIndex].nextDir
                    item.currentIndex++
                    if (item.currentIndex > item.path.lastIndex) {
                        item.goal.output(item.item.type, item.item.quantity)
                        iterator.remove()
                    }
                } else {
                    item.xPixel += ITEM_TRANSPORT_SPEED * GeometryHelper.getXSign(item.dir)
                    item.yPixel += ITEM_TRANSPORT_SPEED * GeometryHelper.getYSign(item.dir)
                }
            }
        }

        fun render() {
            for(item in itemsBeingMoved) {
                Renderer.renderTexture(item.item.type.texture, item.xPixel + 4, item.yPixel + 4, 8, 8)
                Renderer.renderText(item.item.quantity, item.xPixel + 4, item.yPixel + 4)
            }
        }

        override fun remove(resource: ItemType, quantity: Int, to: OutputNode<ItemType>?, checkIfContains: Boolean): Boolean {
            return true
        }

        override fun spaceFor(resource: ItemType, quantity: Int): Boolean {
            return true
        }

        override fun contains(resource: ItemType, quantity: Int): Boolean {
            return true
        }

        override fun clear() {
        }

        companion object {
            const val ITEM_TRANSPORT_SPEED = 1
        }
    }

    private class ItemPath(p: List<IntersectionAndDir>) {

        data class Step(val coord: PixelCoord, val nextDir: Int)

        private val steps: Array<Step>

        init {
            val a = arrayOfNulls<Step>(p.size)
            for (index in p.indices) {
                a[index] = Step(PixelCoord(p[index].intersection.tubeBlock.xPixel, p[index].intersection.tubeBlock.yPixel), p[index].dir)
            }
            steps = a as Array<Step>
        }

        val size: Int
            get() = steps.size

        val lastIndex: Int
            get() = steps.lastIndex

        operator fun get(i: Int): Step {
            return steps[i]
        }

        override fun toString(): String {
            return "\n" + steps.joinToString("\n")
        }
    }
    // when a tube block gets placed, it checks for nearby networks and merges them as appropriate.
    // if there is a block with no network next to it, they create a new one
    // if there is a block with a network next to it, it joins
    // if it has a network and there is a block with a network the smallest network merges into the largest
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