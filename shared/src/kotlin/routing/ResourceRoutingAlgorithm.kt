package routing

import level.pipe.ItemPipeBlock
import level.pipe.PipeBlock
import misc.Geometry
import misc.PixelCoord
import resource.ResourceNode
import serialization.Id
import kotlin.math.absoluteValue

/**
 * @param input the internal network node that the package will start at
 * @param output the internal network node that the package will end at
 */
fun route(input: ResourceNode, output: ResourceNode, network: PipeRoutingNetwork) = route(input.xTile shl 4, input.yTile shl 4, Geometry.getOppositeAngle(input.dir), output, network)

fun route(startXPixel: Int, startYPixel: Int, startDir: Int, output: ResourceNode, network: PipeRoutingNetwork): PackageRoute? {
    val startXTile = startXPixel shr 4
    val startYTile = startYPixel shr 4
    if (startXTile == output.xTile && startYTile == output.yTile) {
        val instructions = mutableListOf<RouteStep>()
        instructions.add(RouteStep(PixelCoord(startXTile shl 4, startYTile shl 4), output.dir))
        val attachedToOutput = output.attachedNode!!
        instructions.add(RouteStep(PixelCoord(attachedToOutput.xTile shl 4, attachedToOutput.yTile shl 4), -1))
        return PackageRoute(instructions.toTypedArray())
    }
    val initialConnections = network.findConnections(startXTile, startYTile)
    val outputTube = network.pipes.first { it.xTile == output.xTile && it.yTile == output.yTile }
    val outputIntersection = network.getIntersection(outputTube)!!
    val startNode = AStarRoutingNode(null, startXTile, startYTile, initialConnections, outputIntersection, -1, 0, heuristic(startXTile, startYTile, outputIntersection.xTile, outputIntersection.yTile))

    val possibleNextNodes = mutableListOf<AStarRoutingNode>()
    val alreadyUsedNodes = mutableListOf<AStarRoutingNode>()
    possibleNextNodes.add(startNode)

    var finalNode: AStarRoutingNode? = null

    main@ while (possibleNextNodes.isNotEmpty()) {
        // next can't be null because it's not empty
        val nextNode = possibleNextNodes.minBy { it.f }!!

        // if we're at the destination
        if (nextNode.xTile == outputIntersection.xTile && nextNode.yTile == outputIntersection.yTile) {
            finalNode = nextNode
            break@main
        }

        val nodeNeighbors = nextNode.getNeighbors()
        for (newNode in nodeNeighbors) {
            // already been moved to
            if (alreadyUsedNodes.any { it.xTile == newNode.xTile && it.yTile == newNode.yTile }) {
                continue
            }
            val alreadyExistingPossibility = possibleNextNodes.firstOrNull { it.xTile == newNode.xTile && it.yTile == newNode.yTile }
            if (alreadyExistingPossibility != null) {
                if (alreadyExistingPossibility.g < newNode.g) {
                    // don't care about this newNode because there's a better path to it
                    continue
                } else if (alreadyExistingPossibility.g > newNode.g) {
                    // the already existing possibility is worse, update it to essentially be the new possibility
                    alreadyExistingPossibility.g = newNode.g
                    alreadyExistingPossibility.h = newNode.h
                    alreadyExistingPossibility.parent = newNode.parent
                    alreadyExistingPossibility.directionFromParent = newNode.directionFromParent
                }
            } else {
                possibleNextNodes.add(newNode)
            }
        }
        possibleNextNodes.remove(nextNode)
        alreadyUsedNodes.add(nextNode)
    }
    if (finalNode != null) {
        val instructions = mutableListOf<RouteStep>()
        val endXPixel = (output.xTile shl 4) + 8 * Geometry.getXSign(output.dir)
        val endYPixel = (output.yTile shl 4) + 8 * Geometry.getYSign(output.dir)
        // move inside the end block
        instructions.add(RouteStep(PixelCoord(endXPixel, endYPixel), -1))
        // add the last step (cuz it gets skipped by the while loop below)
        instructions.add(RouteStep(PixelCoord(finalNode.xTile shl 4, finalNode.yTile shl 4), output.dir))
        while (finalNode!!.parent != null) {
            instructions.add(RouteStep(PixelCoord(finalNode.parent!!.xTile shl 4, finalNode.parent!!.yTile shl 4), finalNode.directionFromParent))
            finalNode = finalNode.parent
        }
        // come out from the start block
        instructions.add(RouteStep(PixelCoord(startXPixel - 8 * Geometry.getXSign(startDir), startYPixel - 8 * Geometry.getYSign(startDir)), startDir))
        instructions.reverse()
        return PackageRoute(instructions.toTypedArray())
    }
    return null
}

private fun heuristic(xTile: Int, yTile: Int, goalXTile: Int, goalYTile: Int): Int {
    return (xTile - goalXTile).absoluteValue + (yTile - goalYTile).absoluteValue
}

data class Intersection(
        @Id(1)
        val pipeBlock: PipeBlock,
        @Id(2)
        var connections: Connections) {

    private constructor() : this(ItemPipeBlock(0, 0), Connections())

    val xTile get() = pipeBlock.xTile
    val yTile get() = pipeBlock.yTile

    override fun equals(other: Any?): Boolean {
        return other is Intersection && other.pipeBlock == pipeBlock
    }

    override fun hashCode(): Int {
        return pipeBlock.hashCode()
    }

    override fun toString(): String {
        return "Intersection at ${pipeBlock.xTile}, ${pipeBlock.yTile}"
    }
}

data class Connections(
        @Id(1)
        val intersections: Array<Intersection?> = arrayOfNulls(4),
        @Id(2)
        val distances: Array<Int?> = arrayOfNulls(4)) {

    private constructor() : this(arrayOf(), arrayOf())

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

class AStarRoutingNode(var parent: AStarRoutingNode? = null, val xTile: Int, val yTile: Int, var connections: Connections, var goal: Intersection, var directionFromParent: Int, var g: Int, var h: Int) {

    val f = h + g

    fun getNeighbors(): List<AStarRoutingNode> {
        val l = mutableListOf<AStarRoutingNode>()
        for (i in 0 until 4) {
            if (connections[i] != null) {
                val newIntersection = connections[i]!!.intersection
                val dist = connections[i]!!.dist
                l.add(AStarRoutingNode(this, newIntersection.xTile, newIntersection.yTile, newIntersection.connections, goal,
                        i,
                        g + dist,
                        heuristic(newIntersection.xTile, newIntersection.yTile, goal.xTile, goal.yTile)))
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

class PackageRoute(
        @Id(1)
        private val steps: Array<RouteStep>) {

    private constructor() : this(arrayOf())

    val size: Int
        get() = steps.size

    val lastIndex: Int
        get() = steps.lastIndex

    operator fun get(i: Int): RouteStep {
        return steps[i]
    }

    fun indexOf(step: RouteStep) = steps.indexOf(step)

    fun withIndex() = steps.withIndex()

    operator fun iterator() = steps.iterator()

    override fun toString(): String {
        return "\n" + steps.joinToString("\n")
    }
}

data class RouteStep(
        @Id(1)
        val position: PixelCoord,
        @Id(2)
        val nextDir: Int) {
    private constructor() : this(PixelCoord(0, 0), 0)
}