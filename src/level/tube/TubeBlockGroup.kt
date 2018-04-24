package level.tube

import graphics.Renderer
import item.Item
import item.ItemType
import level.Level
import main.Game
import misc.GeometryHelper
import misc.PixelCoord
import misc.WeakMutableList
import resource.*
import java.io.DataOutputStream

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
    private val nodes = ResourceNodeGroup("Tube block group $id")

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
        other.nodes.forEach {
            it as ResourceNode<ItemType>
            it.attachedContainer = storage
            if (it !in nodes)
                nodes.add(it)
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
    fun createCorrespondingNodes(nodes: List<ResourceNode<ItemType>>) {
        val new = nodes.map { ResourceNode.createCorresponding(it, storage) }
        new.forEach { Level.add(it) }
        this.nodes.addAll(new)
    }

    /**
     * Creates transfer nodes corresponding to the tube's nodes
     */
    fun createCorrespondingNodes(tubeBlock: TubeBlock) {
        tubeBlock.nodeConnections.forEach { createCorrespondingNodes(it) }
    }

    /**
     * Removes all nodes that were given to the network because of this
     */
    fun removeCorrespondingNodes(t: TubeBlock) {
        val r = nodes.filter { it.xTile != t.xTile && it.yTile != t.yTile }
        nodes.removeAll(r)
        r.forEach { Level.remove(it) }
    }

    fun convertToIntersection(tube: TubeBlock): IntersectionTube? {
        if (isIntersection(tube)) {
            return getIntersection(tube)
        }
        return null
    }

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

    private fun route(input: ResourceNode<*>, output: ResourceNode<*>): ItemPath? {

        return route(input.xTile, input.yTile, output)
    }

    private fun route(startXTile: Int, startYTile: Int, output: ResourceNode<*>): ItemPath? {
        if (startXTile == output.xTile && startYTile == output.yTile) {
            val instructions = mutableListOf<Step>()
            instructions.add(Step(PixelCoord(startXTile shl 4, startYTile shl 4), output.dir))
            instructions.add(Step(PixelCoord(output.attachedNode!!.xTile shl 4, output.attachedNode!!.yTile shl 4), -1))
            return ItemPath(instructions.toTypedArray())
        }
        // temporary means that this node is only an intersection for routing purposes
        val inp = getIntersection(tubes.first { it.xTile == startXTile && it.yTile == startYTile }, true)
        val out = getIntersection(tubes.first { it.xTile == output.xTile && it.yTile == output.yTile }, true)
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

    private data class ItemPackage(var item: Item, var start: ResourceNode<ItemType>, var goal: ResourceNode<ItemType>, var currentIndex: Int, var path: ItemPath, var xPixel: Int, var yPixel: Int, var dir: Int = 0)

    // When an output node tries to input into this, do nothing if there is nowhere to put it
    // Each stack inputted is stored as separate, even if they could be combined. If there is more than 1 stack inputted at a time,
    // split it up and send it with a delay
    class TubeBlockGroupInternalStorage(val parent: TubeBlockGroup) : ResourceContainer<ItemType>(ResourceType.ITEM) {

        private val itemsBeingMoved = mutableListOf<ItemPackage>()

        override fun add(resource: ResourceType, quantity: Int, from: ResourceNode<*>?, checkIfAble: Boolean): Boolean {
            if (!isValid(resource))
                return false
            from as ResourceNode<ItemType>?
            resource as ItemType
            if (from != null) {
                val output = parent.nodes.getPossibleOutputter(resource, quantity, onlyTo = { it != from })
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
                if (!item.goal.canOutputFromContainer(item.item.type, item.item.quantity)) {
                    if (item.start.canOutputFromContainer(item.item.type, item.item.quantity)) {
                        val path = parent.route((item.xPixel shr 4) - GeometryHelper.getXSign(item.dir), (item.yPixel shr 4) - GeometryHelper.getYSign(item.dir), item.start)
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
                        val output = parent.nodes.getPossibleOutputter(item.item.type, item.item.quantity)
                        println("can output to: $output")
                        if (output != null) {
                            val path = parent.route(item.xPixel shr 4, item.yPixel shr 4, output)
                            if (path != null) {
                                item.path = path
                                println(path)
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
                Renderer.renderTextureKeepAspect(item.item.type.texture, item.xPixel + 4, item.yPixel + 4, 8, 8)
                Renderer.renderText(item.item.quantity, item.xPixel + 4, item.yPixel + 4)
                if (Game.DEBUG_TUBE_INFO)
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

        override fun remove(resource: ResourceType, quantity: Int, to: ResourceNode<*>?, checkIfContains: Boolean): Boolean {
            return true
        }

        override fun spaceFor(resource: ResourceType, quantity: Int): Boolean {
            return parent.nodes.canOutput(resource, quantity)
        }

        override fun contains(resource: ResourceType, quantity: Int): Boolean {
            return true
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