package routing

import data.ConcurrentlyModifiableMutableList
import item.Inventory
import item.ItemType
import level.Level
import level.LevelManager
import level.pipe.PipeBlock
import misc.Geometry
import misc.PixelCoord
import resource.ResourceCategory
import resource.ResourceNode
import resource.ResourceType
import serialization.Id
import kotlin.math.absoluteValue
import kotlin.math.sign

abstract class PipeRoutingNetwork(resourceCategory: ResourceCategory, level: Level, private val speed: Int = 1) : ResourceRoutingNetwork(resourceCategory, level) {

    private constructor() : this(ResourceCategory.ITEM, LevelManager.EMPTY_LEVEL)

    @Id(12)
    val pipes = mutableSetOf<PipeBlock>()

    /**
     * The list of [Intersection]s in this network
     */
    @Id(13)
    val intersections = mutableSetOf<Intersection>()

    @Id(11)
    private val packages = ConcurrentlyModifiableMutableList<PipeRoutingPackage>()

    val size
        get() = pipes.size

    fun addPipe(pipe: PipeBlock) {
        pipes.add(pipe)
    }

    fun removePipe(pipe: PipeBlock) {
        pipes.remove(pipe)
        val toRemove = internalNodes.filter { it.xTile == pipe.xTile && it.yTile == pipe.yTile }
        toRemove.forEach { level.remove(it); internalNodes.remove(it) }
        val adjacentPipes = pipes.filter { (it.xTile - pipe.xTile).absoluteValue + (it.yTile - pipe.yTile).absoluteValue == 1 }
        if (adjacentPipes.size > 1) {
            // if there was only 1 or 0 pipe this can't possibly change the network
            val intersection = getIntersection(pipe)
            if (intersection != null) {
                // if this has 2 connections it
            }
        }
        if (pipe.intersection != null) {
            removeIntersection(pipe.intersection!!)
        }
    }

    fun getIntersection(t: PipeBlock): Intersection? {
        return intersections.firstOrNull { it.pipeBlock == t }
    }

    private fun getPipeAt(xTile: Int, yTile: Int) = pipes.firstOrNull { it.xTile == xTile && it.yTile == yTile }

    override fun mergeIntoThis(other: ResourceRoutingNetwork) {
        super.mergeIntoThis(other)
        if (other is PipeRoutingNetwork) {
            for (pipe in other.pipes) {
                pipe.network = this
                pipes.add(pipe)
            }
            intersections.addAll(other.intersections)
            other.packages.forEach { packages.add(it) }
        }
    }

    /**
     * Updates the connections of the given intersection
     */
    fun updateIntersection(pipe: PipeBlock) {
        var intersection = intersections.firstOrNull { it.pipeBlock == pipe }
        if (intersection == null) {
            intersection = Intersection(pipe, Connections())
            intersections.add(intersection)
        }
        intersection.connections = findConnections(intersection)
    }

    fun removeIntersection(intersection: Intersection) {
        intersections.remove(intersection)
        for (i in 0..3) {
            val connected = intersection.connections.intersections[i]
            if (connected != null) {
                connected.connections = findConnections(connected)
            }
        }
    }

    /**
     * @return the [Connections] object describing the intersections in each direction and their distance to this [xTile], [yTile].
     * This will also create new intersections as needed (if there should be one but isn't)
     */
    fun findConnections(xTile: Int, yTile: Int): Connections {
        val connecIntersections = arrayOfNulls<Intersection>(4)
        val connecDists = arrayOfNulls<Int>(4)
        val pipe = getPipeAt(xTile, yTile)
                ?: throw IllegalArgumentException("Can't find the connections at $xTile, $yTile because there is no pipe in the network there")
        for (i in 0..3) {
            var currentPipe = pipe.pipeConnections[i]
            var dist = 1
            // Iterate down the pipe until there are no more connections
            while (currentPipe != null && !currentPipe.shouldBeIntersection()) {
                currentPipe = currentPipe.pipeConnections[i]
                dist++
            }
            // currentPipe is either null or an intersection
            if (currentPipe != pipe && currentPipe != null) {
                connecIntersections[i] = getIntersection(currentPipe)
                connecDists[i] = dist
            }
        }
        return Connections(connecIntersections, connecDists)
    }

    /**
     * @return the [Connections] object describing the intersections in each direction and their distance to this [intersection].
     * This will create new intersections as needed (if there should be one but isn't), and it will update the intersections
     * this is connected to so that they are current relative to this one.
     */
    fun findConnections(intersection: Intersection): Connections {
        val connecIntersections = arrayOfNulls<Intersection>(4)
        val connecDists = arrayOfNulls<Int>(4)
        for (i in 0..3) {
            var currentPipe = intersection.pipeBlock.pipeConnections[i]
            var dist = 1
            // Iterate down the tube until there are no more connections
            while (currentPipe != null && !currentPipe.shouldBeIntersection()) {
                currentPipe = currentPipe.pipeConnections[i]
                dist++
            }
            // currentPipe is either null or an intersection
            if (currentPipe != intersection.pipeBlock && currentPipe != null) {
                var newIntersection = getIntersection(currentPipe)
                if (newIntersection == null) {
                    updateIntersection(currentPipe)
                    newIntersection = getIntersection(currentPipe)!!
                }
                newIntersection.connections.distances[Geometry.getOppositeAngle(i)] = dist
                newIntersection.connections.intersections[Geometry.getOppositeAngle(i)] = intersection
                connecIntersections[i] = newIntersection
                connecDists[i] = dist
            }
        }
        return Connections(connecIntersections, connecDists)
    }

    override fun transferResources(type: ResourceType, quantity: Int, from: ResourceNode, to: ResourceNode): Boolean {
        val route = route(from, to, this)
        if (route != null) {
            val expectSuccess = to.attachedNode!!.attachedContainer.expect(type, quantity)
            if(!expectSuccess) {
                throw Exception("Wasn't able to expect resources in ${to.attachedNode!!.attachedContainer} successfully, but was still told to transfer resources there")
            }
            nodesSentTo.add(to)
            if (nodesSentTo.size == attachedNodes.filter { it.behavior.allowIn.possible() != null }.size) {
                // if we've sent to all internal nodes
                nodesSentTo.clear()
            }
            packages.add(PipeRoutingPackage(from, to, type, quantity, route))
            return true
        }
        return false
    }

    private fun reroutePackage(pack: PipeRoutingPackage) {
        val newDestination = findDestinationFor(pack.type, pack.quantity)
        if (newDestination != null) {
            val route = route(pack.to, newDestination, this)
            if (route != null) {
                if (newDestination.attachedNode!!.attachedContainer.expect(pack.type, pack.quantity)) {
                    pack.from = pack.to
                    pack.to.attachedContainer.cancelExpectation(pack.type, pack.quantity)
                    pack.to = newDestination
                    pack.route = route
                    val currentRouteStep: RouteStep?
                    //if the package should be moving on the x axis, look for the nearest step in the same y
                    if (Geometry.getXSign(pack.dir) != 0) {
                        // the route step that is in the direction that is on the same Y as this and is in the correct direction of movement, and is closest to this
                        currentRouteStep = route.iterator().asSequence()
                                .filter { (it.position.yPixel == pack.position.yPixel) && (it.position.xPixel - pack.position.xPixel).sign == Geometry.getXSign(pack.dir) }
                                .minBy { (it.position.xPixel - pack.position.xPixel).absoluteValue }
                    } else {
                        currentRouteStep = route.iterator().asSequence()
                                .filter { (it.position.xPixel == pack.position.xPixel) && (it.position.yPixel - pack.position.yPixel).sign == Geometry.getYSign(pack.dir) }
                                .minBy { (it.position.yPixel - pack.position.yPixel).absoluteValue }
                    }
                    pack.routeStepIndex = route.indexOf(currentRouteStep!!)
                    pack.dir = route[pack.routeStepIndex - 1].nextDir
                }
            }
        } else {
            // no dest available
            pack.awaitingRoute = true
        }
    }

    override fun update() {

        fun atDestination(p: PipeRoutingPackage): Boolean {
            if (p.position.manhattanDistance(p.route[p.route.lastIndex].position) <= speed) {
                return true
            }
            return false
        }

        packages.forEach { pack ->
            if (pack.awaitingRoute) {
                reroutePackage(pack)
            } else if (atDestination(pack)) {
                if (pack.to.attachedNode != null && pack.to.attachedNode!!.canInput(pack.type, pack.quantity)) {
                    pack.to.attachedNode!!.input(pack.type, pack.quantity, false)
                    packages.remove(pack)
                } else {
                    reroutePackage(pack)
                }
            } else {
                if (pack.position.manhattanDistance(pack.currentRouteStep.position) <= speed) {
                    pack.dir = pack.currentRouteStep.nextDir
                    pack.routeStepIndex++
                }
                pack.position.xPixel += Geometry.getXSign(pack.dir) * speed
                pack.position.yPixel += Geometry.getYSign(pack.dir) * speed
            }
        }
    }

    override fun render() {
        packages.forEach {
            it.type.icon.render(it.position.xPixel, it.position.yPixel + 5, 12, 12, true)
        }
    }

    data class PipeRoutingPackage(
            @Id(1)
            var from: ResourceNode,
            @Id(8)
            var to: ResourceNode,
            @Id(2)
            var type: ResourceType,
            @Id(3)
            var quantity: Int,
            @Id(4)
            var route: PackageRoute,
            @Id(5)
            var routeStepIndex: Int = 0,
            @Id(6)
            var position: PixelCoord = PixelCoord(route[0].position.xPixel, route[0].position.yPixel),
            @Id(7)
            var dir: Int = route[0].nextDir,
            @Id(8)
            var awaitingRoute: Boolean = false) {

        private constructor() : this(ResourceNode(0, 0, 0, ResourceCategory.ITEM, Inventory(0, 0), LevelManager.EMPTY_LEVEL),
                ResourceNode(0, 0, 0, ResourceCategory.ITEM, Inventory(0, 0), LevelManager.EMPTY_LEVEL), ItemType.ERROR, 0, PackageRoute(arrayOf()), 0, PixelCoord(0, 0), 0)

        val currentRouteStep
            get() = route[routeStepIndex]
    }
}