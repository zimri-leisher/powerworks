package routing

import level.tube.TubeBlock
import misc.Geometry
import misc.PixelCoord
import resource.ResourceNode

/**
 * @param input the internal network node that the package will start at
 * @param output the internal network node that the package will end at
 */
fun route(input: ResourceNode, output: ResourceNode, network: TubeRoutingNetwork) = route(input.xTile shl 4, input.yTile shl 4, output, network)

fun route(startXPixel: Int, startYPixel: Int, output: ResourceNode, network: TubeRoutingNetwork): PackageRoute? {
    val startXTile = startXPixel shr 4
    val startYTile = startYPixel shr 4
    if (startXTile == output.xTile && startYTile == output.yTile) {
        val instructions = mutableListOf<RouteStep>()
        instructions.add(RouteStep(PixelCoord(startXTile shl 4, startYTile shl 4), output.dir))
        val attachedToOutput = output.attachedNodes.first()
        instructions.add(RouteStep(PixelCoord(attachedToOutput.xTile shl 4, attachedToOutput.yTile shl 4), -1))
        return PackageRoute(instructions.toTypedArray())
    }
    val initialConnections = network.findConnections(startXTile, startYTile)
    val outputTube = network.tubes.first { it.xTile == output.xTile && it.yTile == output.yTile }
    val outputIntersection = network.getIntersection(outputTube)!!
    val startNode = AStarRoutingNode(null, startXTile, startYTile, initialConnections, outputIntersection, -1, 0, 0)

    val possibleNextNodes = mutableListOf<AStarRoutingNode>()
    val alreadyUsedNodes = mutableListOf<AStarRoutingNode>()
    possibleNextNodes.add(startNode)

    var finalNode: AStarRoutingNode? = null

    main@ while (possibleNextNodes.isNotEmpty()) {
        // next can't be null because it's not empty
        val nextNode = possibleNextNodes.minBy { it.f }!!

        possibleNextNodes.remove(nextNode)

        val nodeChildren = nextNode.calculateChildren()
        for (nodeChild in nodeChildren) {
            if (nodeChild.xTile == outputIntersection.tubeBlock.xTile && nodeChild.yTile == outputIntersection.tubeBlock.yTile) {
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
        val instructions = mutableListOf<RouteStep>()
        val endXTile = output.xTile + Geometry.getXSign(output.dir)
        val endYTile = output.yTile + Geometry.getYSign(output.dir)
        instructions.add(RouteStep(PixelCoord(endXTile shl 4, endYTile shl 4), -1))
        instructions.add(RouteStep(PixelCoord(finalNode.xTile shl 4, finalNode.yTile shl 4), output.dir))
        while (finalNode!!.parent != null) {
            instructions.add(RouteStep(PixelCoord(finalNode.parent!!.xTile shl 4, finalNode.parent!!.yTile shl 4), finalNode.directionFromParent))
            finalNode = finalNode.parent
        }
        instructions.reverse()
        return PackageRoute(instructions.toTypedArray())
    }
    return null
}

data class Intersection(val tubeBlock: TubeBlock, var connections: Connections) {

    val xTile get() = tubeBlock.xTile
    val yTile get() = tubeBlock.yTile

    override fun equals(other: Any?): Boolean {
        return other is Intersection && other.tubeBlock == tubeBlock
    }

    override fun hashCode(): Int {
        return tubeBlock.hashCode()
    }

    override fun toString(): String {
        return "Intersection at ${tubeBlock.xTile}, ${tubeBlock.yTile}"
    }
}

data class Connections(val intersections: Array<Intersection?> = arrayOfNulls(4), val distances: Array<Int?> = arrayOfNulls(4)) {
    operator fun get(i: Int) = if (intersections[i] == null) null else Pair(intersections[i]!!, distances[i]!!)

    operator fun set(i: Int, value: Pair<Intersection, Int>?) {
        intersections[i] = value?.intersection
        distances[i] = value?.dist
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Connections
        if (!intersections.contentEquals(other.intersections)) return false
        if (!distances.contentEquals(other.distances)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = intersections.contentHashCode()
        result = 31 * result + distances.contentHashCode()
        return result
    }
}

val Pair<Intersection, Int>.intersection get() = first
val Pair<Intersection, Int>.dist get() = second

class AStarRoutingNode(val parent: AStarRoutingNode? = null, val xTile: Int, val yTile: Int, val connections: Connections, val goal: Intersection, val directionFromParent: Int, val g: Int, val h: Int) {

    val f = h + g

    fun calculateChildren(): List<AStarRoutingNode> {
        val l = mutableListOf<AStarRoutingNode>()
        for (i in 0 until 4) {
            if (connections[i] != null) {
                val newIntersection = connections[i]!!.intersection
                val dist = connections[i]!!.dist
                l.add(AStarRoutingNode(this, newIntersection.xTile, newIntersection.yTile, newIntersection.connections, goal,
                        i,
                        g + dist,
                        Math.abs(goal.tubeBlock.xTile - newIntersection.tubeBlock.xTile) + Math.abs(goal.tubeBlock.yTile - newIntersection.tubeBlock.yTile)))
            }
        }
        return l
    }

    override fun equals(other: Any?): Boolean {
        return other is AStarRoutingNode && other.xTile == xTile && other.yTile == yTile && other.g == g && other.h == h && other.directionFromParent == directionFromParent
    }

    override fun hashCode(): Int {
        var result = parent?.hashCode() ?: 0
        result = 31 * result + xTile
        result = 31 * result + yTile
        result = 31 * result + connections.hashCode()
        result = 31 * result + goal.hashCode()
        result = 31 * result + directionFromParent
        result = 31 * result + g
        result = 31 * result + h
        result = 31 * result + f
        return result
    }

}

class PackageRoute(private val steps: Array<RouteStep>) {

    val size: Int
        get() = steps.size

    val lastIndex: Int
        get() = steps.lastIndex

    operator fun get(i: Int): RouteStep {
        return steps[i]
    }

    fun withIndex() = steps.withIndex()

    operator fun iterator() = steps.iterator()

    override fun toString(): String {
        return "\n" + steps.joinToString("\n")
    }
}

data class RouteStep(val loc: PixelCoord, val nextDir: Int)