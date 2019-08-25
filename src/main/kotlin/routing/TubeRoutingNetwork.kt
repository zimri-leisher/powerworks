package routing

import data.ConcurrentlyModifiableMutableList
import level.tube.TubeBlock
import misc.Geometry
import misc.PixelCoord
import resource.ResourceCategory
import resource.ResourceNode
import resource.ResourceType

class TubeRoutingNetwork : ResourceRoutingNetwork(ResourceCategory.ITEM) {

    val tubes = mutableSetOf<TubeBlock>()

    /**
     * The list of [Intersection]s in this network
     */
    val intersections = mutableSetOf<Intersection>()
    private val packages = ConcurrentlyModifiableMutableList<RoutingPackage>()

    val size
        get() = tubes.size

    fun addTube(tube: TubeBlock) {
        tubes.add(tube)
    }

    fun removeTube(tube: TubeBlock) {
        tubes.remove(tube)
    }

    fun getIntersection(t: TubeBlock): Intersection? {
        return intersections.firstOrNull { it.tubeBlock == t }
    }

    fun mergeIntoThis(other: TubeRoutingNetwork) {
        other.tubes.forEach {
            if (it !in tubes) {
                tubes.add(it)
                it.network = this
            }
        }
        other.attachedNodes.forEach {
            it.attachedContainer = this
            if (it !in attachedNodes)
                attachedNodes.add(it)
        }
    }

    private fun getTubeAt(xTile: Int, yTile: Int) = tubes.firstOrNull { it.xTile == xTile && it.yTile == yTile }

    /**
     * Updates the connections of the given intersection
     */
    fun updateIntersection(tube: TubeBlock) {
        var intersection = intersections.firstOrNull { it.tubeBlock == tube }
        if (intersection == null) {
            intersection = Intersection(tube, Connections())
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
        val tube = getTubeAt(xTile, yTile)
                ?: throw IllegalArgumentException("Can't find the connections at $xTile, $yTile because there is no tube in the network there")
        for (i in 0..3) {
            var currentTube = tube.tubeConnections[i]
            var dist = 1
            // Iterate down the tube until there are no more connections
            while (currentTube != null && !currentTube.shouldBeIntersection()) {
                currentTube = currentTube.tubeConnections[i]
                dist++
            }
            // currentTube is either null or an intersection
            if (currentTube != tube && currentTube != null) {
                connecIntersections[i] = getIntersection(currentTube)
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
            var currentTube = intersection.tubeBlock.tubeConnections[i]
            var dist = 1
            // Iterate down the tube until there are no more connections
            while (currentTube != null && !currentTube.shouldBeIntersection()) {
                currentTube = currentTube.tubeConnections[i]
                dist++
            }
            // currentTube is either null or an intersection
            if (currentTube != intersection.tubeBlock && currentTube != null) {
                var newIntersection = getIntersection(currentTube)
                if (newIntersection == null) {
                    updateIntersection(currentTube)
                    newIntersection = getIntersection(currentTube)!!
                }
                newIntersection.connections.distances[Geometry.getOppositeAngle(i)] = dist
                newIntersection.connections.intersections[Geometry.getOppositeAngle(i)] = intersection
                connecIntersections[i] = newIntersection
                connecDists[i] = dist
            }
        }
        return Connections(connecIntersections, connecDists)
    }

    override fun onAddResource(resource: ResourceType, quantity: Int, from: ResourceNode?) {
        val to = attachedNodes.getInputter(resource, quantity)
    }

    override fun onRemoveResource(resource: ResourceType, quantity: Int, to: ResourceNode?) {

    }

    override fun sendTo(node: ResourceNode, type: ResourceType, quantity: Int) {
        val sendTo = if (!node.isInternalNetworkNode) node.attachedNodes.first { it.isInternalNetworkNode } else node
        val outputters = attachedNodes.getOutputters(type, quantity, { it != sendTo })
        val takeFrom = outputters.getForceOutputter(type, quantity) ?: outputters.firstOrNull()
        if (takeFrom != null) {
            val route = route(takeFrom, sendTo, this)
            if (route != null) {
                sendTo.attachedNodes.first().attachedContainer.expect(type, quantity)
                packages.add(RoutingPackage(takeFrom, node, type, quantity, route))
            }
        }
    }

    override fun takeFrom(node: ResourceNode, type: ResourceType, quantity: Int) {
        val takeFrom = if (!node.isInternalNetworkNode) node.attachedNodes.first { it.isInternalNetworkNode } else node
        val sendTo = internalNodes.filter { it != takeFrom }.getOutputter(type, quantity)
        if (sendTo != null) {
            val route = route(takeFrom, sendTo, this)
            if (route != null) {
                if (sendTo.attachedNodes.first().attachedContainer.expect(type, quantity)) {
                    takeFrom.attachedNodes.first().attachedContainer.remove(type, quantity, sendTo)
                    packages.add(RoutingPackage(takeFrom, sendTo, type, quantity, route))
                }
            }
        }
    }

    override fun update() {

        fun atDestination(p: RoutingPackage): Boolean {
            if (p.position.xPixel == (p.to.xTile shl 4) && p.position.yPixel == (p.to.yTile shl 4)) {
                return true
            }
            return false
        }

        packages.forEach {
            if (atDestination(it)) {
                packages.remove(it)
                if (!it.to.attachedNodes.first().input(it.type, it.quantity)) {
                    println("couldnt input at end")
                }
            } else {
                if (it.position == it.currentRouteStep.loc) {
                    it.routeStepIndex++
                    it.dir = it.currentRouteStep.nextDir
                }
                it.position.xPixel += Geometry.getXSign(it.dir)
                it.position.yPixel += Geometry.getYSign(it.dir)
            }
        }
    }

    override fun render() {
        packages.forEach {
            it.type.icon.render(it.position.xPixel, it.position.yPixel)
        }
    }

    private data class RoutingPackage(val from: ResourceNode,
                                      val to: ResourceNode,
                                      val type: ResourceType,
                                      val quantity: Int,
                                      val route: PackageRoute,
                                      var routeStepIndex: Int = 0,
                                      var position: PixelCoord = PixelCoord(from.xTile shl 4, from.yTile shl 4),
                                      var dir: Int = -1) {
        val currentRouteStep
            get() = route[routeStepIndex]
    }
}