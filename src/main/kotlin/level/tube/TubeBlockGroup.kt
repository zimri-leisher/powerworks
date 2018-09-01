package level.tube

import graphics.Renderer
import item.Item
import item.ItemType
import level.Level
import main.Game
import misc.GeometryHelper
import misc.PixelCoord
import data.WeakMutableList
import main.DebugCode
import resource.*
import java.io.DataOutputStream

class TubeBlockGroup {

    private val tubes = mutableListOf<TubeBlock>()
    val intersections = mutableListOf<IntersectionTube>()
    private val storage = TubeBlockGroupInternalStorage(this)
    val id = nextId++
    private val nodes = ResourceNodeGroup("Tube block group $id")

    val size
        get() = tubes.size

    init {
        ALL.add(this)
    }

    /**
     * Combines the two and sets all the appropriate values for their tubes, inputs and outputs
     */
    fun merge(other: TubeBlockGroup) {
        // so that they don't bother making or removing connections, because it would just remove a connection and add it again
        other.tubes.forEach {
            if (it !in tubes) {
                tubes.add(it)
                it.group = this
            }
        }
        other.nodes.forEach {
            it as ResourceNode<ItemType>
            it.attachedContainer = storage
            if (it !in nodes)
                nodes.add(it)
        }
    }

    fun addTube(t: TubeBlock) {
        tubes.add(t)
    }

    fun removeTube(t: TubeBlock) {
        tubes.remove(t)
    }

    /**
     * Creates transfer nodes corresponding the inputted nodes
     */
    fun createCorrespondingNodes(nodes: List<ResourceNode<ItemType>>) {
        val new = nodes.map { ResourceNode.createCorresponding(it, storage) }
        for (newNode in new) {
            if (newNode !in this.nodes) {
                this.nodes.add(newNode)
                Level.add(newNode)
            }
        }
    }

    /**
     * Removes all nodes that were given to the network because of this
     */
    fun removeCorrespondingNodes(t: TubeBlock) {
        val r = nodes.filter { it.xTile != t.xTile && it.yTile != t.yTile }
        nodes.removeAll(r)
        r.forEach { Level.remove(it) }
    }

    /**
     * If the tube is an intersection and it was not already in the network, it will create the intersection, attach it, add it to the network and return it.
     * Otherwise it will return the old one that corresponded to the tube
     * @return the intersection, null if the tube was not an intersection
     */
    fun convertToIntersection(tube: TubeBlock): IntersectionTube? {
        if (isIntersection(tube)) {
            return getIntersection(tube)
        }
        return null
    }

    /**
     * @param isTemporary if true, this intersection is only an intersection because of routing purposes (used when you are inserting
     * an item at an arbitrary point in the network)
     */
    private fun getIntersection(tube: TubeBlock, isTemporary: Boolean = false): IntersectionTube {
        val i = intersections.firstOrNull { it.tubeBlock == tube }
        if (i != null) {
            return i
        } else {
            val t = IntersectionTube(tube, arrayOfNulls(4))
            if (!isTemporary)
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

    class RoutingNode(val parent: RoutingNode? = null, val goal: IntersectionTube, val intersection: IntersectionTube, val directionFromParent: Int, val g: Int, val h: Int) {
        val xTile: Int
            get() = intersection.tubeBlock.xTile
        val yTile: Int
            get() = intersection.tubeBlock.yTile

        val f = h + g

        fun getChildren(): List<RoutingNode> {
            val l = mutableListOf<RoutingNode>()
            for (i in 0 until 4) {
                if (intersection.connectedTo[i] != null) {
                    val newIntersection = intersection.connectedTo[i]!!.intersection
                    val dist = intersection.connectedTo[i]!!.dist
                    l.add(RoutingNode(this, goal, newIntersection,
                            i,
                            g + dist,
                            Math.abs(goal.tubeBlock.xTile - newIntersection.tubeBlock.xTile) + Math.abs(goal.tubeBlock.yTile - newIntersection.tubeBlock.yTile)))
                }
            }
            return l
        }

        override fun equals(other: Any?): Boolean {
            return other is RoutingNode && other.xTile == xTile && other.yTile == yTile && other.g == g && other.h == h && other.directionFromParent == directionFromParent
        }

        override fun toString(): String {
            return "$xTile, $yTile, dir from parent: $directionFromParent"
        }

    }

    data class Step(val coord: PixelCoord, val nextDir: Int)

    private fun route(input: ResourceNode<*>, output: ResourceNode<*>) = route(input.xTile, input.yTile, output)

    private fun route(startXTile: Int, startYTile: Int, output: ResourceNode<*>): ItemPath? {
        if (startXTile == output.xTile && startYTile == output.yTile) {
            val instructions = mutableListOf<Step>()
            instructions.add(Step(PixelCoord(startXTile shl 4, startYTile shl 4), output.dir))
            instructions.add(Step(PixelCoord(output.attachedNode!!.xTile shl 4, output.attachedNode!!.yTile shl 4), -1))
            return ItemPath(instructions.toTypedArray())
        }
        // temporary means that this node is only an intersection for routing purposes
        // the reason we are doing isAdjacentorIntersecting here is because this must also accunt for items that are inside of the blocks
        // they are about to enter
        val inputTube = tubes.first { it.xTile == startXTile && it.yTile == startYTile }
        val outputTube = tubes.first { it.xTile == output.xTile && it.yTile == output.yTile }
        val inp = getIntersection(inputTube, true)
        val out = getIntersection(outputTube, true)
        val startNode = RoutingNode(null, out, inp, -1, 0, 0)

        val possibleNextNodes = mutableListOf<RoutingNode>()
        val alreadyUsedNodes = mutableListOf<RoutingNode>()
        possibleNextNodes.add(startNode)

        var finalNode: RoutingNode? = null

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
            val instructions = mutableListOf<Step>()
            val finalIntersection = finalNode.intersection
            val endXTile = output.xTile + GeometryHelper.getXSign(output.dir)
            val endYTile = output.yTile + GeometryHelper.getYSign(output.dir)
            instructions.add(Step(PixelCoord(endXTile shl 4, endYTile shl 4), -1))
            instructions.add(Step(PixelCoord(finalIntersection.tubeBlock.xPixel, finalIntersection.tubeBlock.yPixel), output.dir))
            while (finalNode!!.parent != null) {
                instructions.add(Step(PixelCoord(finalNode.parent!!.intersection.tubeBlock.xPixel, finalNode.parent!!.intersection.tubeBlock.yPixel), finalNode.directionFromParent))
                finalNode = finalNode.parent
            }
            instructions.reverse()
            return ItemPath(instructions.toTypedArray())
        }
        return null
    }

    fun update() {
        storage.update()
    }

    fun render() {
        storage.render()
    }

    fun save(out: DataOutputStream) {

    }

    override fun toString() = "Tube block group $id"

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

    private fun getNearestTube(item: ItemPackage) = tubes.firstOrNull { it.xTile == item.xPixel shr 4 && it.yTile == item.yPixel shr 4 } ?: tubes.first { GeometryHelper.isAdjacentOrIntersecting((item.xPixel shr 4) - GeometryHelper.getXSign(item.dir), (item.yPixel shr 4) - GeometryHelper.getYSign(item.dir), it.xTile, it.yTile) }

    private data class ItemPackage(var item: Item, var start: ResourceNode<ItemType>, var goal: ResourceNode<ItemType>, var currentIndex: Int, var path: ItemPath, var xPixel: Int, var yPixel: Int, var dir: Int = 0)

    // When an output node tries to input into this, do nothing if there is nowhere to put it
    // Each stack inputted is stored as separate, even if they could be combined. If there is more than 1 stack inputted at a time,
    // split it up and send it with a delay
    class TubeBlockGroupInternalStorage(val parent: TubeBlockGroup) : ResourceContainer<ItemType>(ResourceCategory.ITEM) {

        override val totalQuantity: Int
            get() = itemsBeingMoved.sumBy { it.item.quantity }

        private val itemsBeingMoved = mutableListOf<ItemPackage>()

        override fun add(resource: ResourceType, quantity: Int, from: ResourceNode<*>?, checkIfAble: Boolean): Boolean {
            if (checkIfAble)
                if (!canAdd(resource, quantity))
                    return false
            from as ResourceNode<ItemType>?
            resource as ItemType
            if (from != null) {
                val output = parent.nodes.getOutputter(resource, quantity, { it != from }, false)
                if (output != null) {
                    val t = parent.route(from, output)
                    if (t != null) {
                        val p = ItemPackage(Item(resource, quantity), from, output, 0, t, from.attachedNode!!.xTile shl 4, from.attachedNode!!.yTile shl 4, GeometryHelper.getOppositeAngle(from.dir))
                        itemsBeingMoved.add(p)
                        return true
                    }
                }
            }
            return false
        }

        fun update() {
            val iterator = itemsBeingMoved.iterator()
            for (item in iterator) {
                if(!item.goal.couldOuput(item.item.type, item.item.quantity)) {

                }
                // already know it contains the resources so no need to use canOutput
                if (!item.goal.couldOuput(item.item.type, item.item.quantity)) {
                    if (item.start.canInput(item.item.type, item.item.quantity)) {
                        val nearestTubeToItem = parent.getNearestTube(item)
                        val path = parent.route(nearestTubeToItem.xTile, nearestTubeToItem.yTile, item.start)
                        if (path != null) {
                            item.path = path
                            val dir = GeometryHelper.getDir((item.xPixel shr 4) - (item.path[0].coord.xPixel shr 4), (item.yPixel shr 4) - (item.path[0].coord.yPixel shr 4))
                            item.dir = dir
                            val goal = item.start
                            item.start = item.goal
                            item.goal = goal
                            item.currentIndex = 0
                        }
                    } else {
                        val output = parent.nodes.getOutputter(item.item.type, item.item.quantity)
                        if (output != null) {
                            val nearestTubeToItem = parent.getNearestTube(item)
                            val path = parent.route(nearestTubeToItem.xTile, nearestTubeToItem.yTile, output)
                            if (path != null) {
                                item.path = path
                                item.dir = item.path[0].nextDir
                                item.start = item.goal
                                item.goal = output
                                item.currentIndex = 0
                            }
                        }
                    }
                } else if (item.xPixel == item.path[item.currentIndex].coord.xPixel && item.yPixel == item.path[item.currentIndex].coord.yPixel) {
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
            for (item in itemsBeingMoved) {
                Renderer.renderTextureKeepAspect(item.item.type.icon, item.xPixel + 4, item.yPixel, 8, 8)
                Renderer.renderText(item.item.quantity, item.xPixel + 4, item.yPixel)
                if (Game.currentDebugCode == DebugCode.TUBE_INFO)
                    renderPath(item)
            }
        }

        private fun renderPath(p: ItemPackage) {
            var lastStep: Step? = null
            for ((i, step) in p.path.withIndex()) {
                if (i >= p.currentIndex && lastStep != null) {
                    Renderer.renderFilledRectangle(lastStep.coord.xPixel + 8, lastStep.coord.yPixel + 8, step.coord.xPixel - lastStep.coord.xPixel + 4, step.coord.yPixel - lastStep.coord.yPixel + 4)
                }
                lastStep = step
            }
        }

        override fun remove(resource: ResourceType, quantity: Int, to: ResourceNode<*>?, checkIfAble: Boolean): Boolean {
            return true
        }

        override fun spaceFor(resource: ItemType, quantity: Int): Boolean {
            return parent.nodes.canOutput(resource, quantity, mustContainEnough = false)
        }

        override fun contains(resource: ItemType, quantity: Int): Boolean {
            var q = 0
            for (i in itemsBeingMoved) {
                if (i.item.type == resource)
                    q += i.item.quantity
            }
            return q >= quantity
        }

        override fun clear() {
        }

        override fun copy(): ResourceContainer<ItemType> {
            return TubeBlockGroupInternalStorage(parent)
        }

        override fun toList(): ResourceList {
            val map = mutableMapOf<ResourceType, Int>()
            for (i in itemsBeingMoved) {
                map.put(i.item.type, i.item.quantity)
            }
            return ResourceList(map)
        }

        override fun getQuantity(resource: ResourceType): Int {
            var q = 0
            itemsBeingMoved.forEach { if (it.item.type == resource) q += it.item.quantity }
            return q
        }

        companion object {
            const val ITEM_TRANSPORT_SPEED = 1
        }
    }

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

    private class ItemPath(private val steps: Array<Step>) {

        val size: Int
            get() = steps.size

        val lastIndex: Int
            get() = steps.lastIndex

        operator fun get(i: Int): Step {
            return steps[i]
        }

        fun withIndex() = steps.withIndex()

        operator fun iterator() = steps.iterator()

        override fun toString(): String {
            return "\n" + steps.joinToString("\n")
        }
    }
}