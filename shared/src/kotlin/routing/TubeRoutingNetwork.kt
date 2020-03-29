package routing

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag
import data.ConcurrentlyModifiableMutableList
import item.Inventory
import item.ItemType
import level.Level
import level.LevelManager
import level.tube.TubeBlock
import misc.Geometry
import misc.PixelCoord
import resource.*
import serialization.Id

class TubeRoutingNetwork(level: Level) : ResourceRoutingNetwork(ResourceCategory.ITEM, level) {

    private constructor() : this(LevelManager.EMPTY_LEVEL)

    @Id(12)
    val tubes = mutableSetOf<TubeBlock>()

    /**
     * The list of [Intersection]s in this network
     */
    @Id(10)
    val intersections = mutableSetOf<Intersection>()
    @Id(11)
    private val packages = ConcurrentlyModifiableMutableList<TubeRoutingPackage>()

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

    private fun getTubeAt(xTile: Int, yTile: Int) = tubes.firstOrNull { it.xTile == xTile && it.yTile == yTile }

    override fun mergeIntoThis(other: ResourceRoutingNetwork) {
        super.mergeIntoThis(other)
        if(other is TubeRoutingNetwork) {
            tubes.addAll(other.tubes)
            intersections.addAll(other.intersections)
            other.packages.forEach { packages.add(it) }
        }
    }

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

    override fun transferResources(type: ResourceType, quantity: Int, from: ResourceNode, to: ResourceNode): Boolean {
        val route = route(from, to, this)
        if (route != null) {
            if (to.attachedNode!!.attachedContainer.expect(type, quantity)) {
                packages.add(TubeRoutingPackage(from, to, type, quantity, route))
                return true
            }
        }
        return false
    }

    override fun update() {

        fun atDestination(p: TubeRoutingPackage): Boolean {
            if (p.position.xPixel == (p.to.xTile shl 4) && p.position.yPixel == (p.to.yTile shl 4)) {
                return true
            }
            return false
        }

        packages.forEach {
            if (atDestination(it)) {
                packages.remove(it)
                if (!it.to.attachedNode!!.input(it.type, it.quantity)) {
                }
            } else {
                if (it.position == it.currentRouteStep.loc) {
                    it.dir = it.currentRouteStep.nextDir
                    it.routeStepIndex++
                }
                it.position.xPixel += Geometry.getXSign(it.dir)
                it.position.yPixel += Geometry.getYSign(it.dir)
            }
        }
    }

    override fun render() {
        packages.forEach {
            it.type.icon.render(it.position.xPixel, it.position.yPixel + 4, 12, 12, true)
        }
    }

    data class TubeRoutingPackage(
            @Id(1)
            val from: ResourceNode,
            @Id(8)
            val to: ResourceNode,
            @Id(2)
            val type: ResourceType,
            @Id(3)
            val quantity: Int,
            @Id(4)
            val route: PackageRoute,
            @Id(5)
            var routeStepIndex: Int = 0,
            @Id(6)
            var position: PixelCoord = PixelCoord(from.xTile shl 4, from.yTile shl 4),
            @Id(7)
            var dir: Int = route[routeStepIndex].nextDir) {

        private constructor() : this(ResourceNode(0,0,0, ResourceCategory.ITEM, Inventory(0,0), LevelManager.EMPTY_LEVEL),
                ResourceNode(0,0,0,ResourceCategory.ITEM, Inventory(0,0), LevelManager.EMPTY_LEVEL), ItemType.ERROR, 0, PackageRoute(arrayOf()), 0, PixelCoord(0, 0), 0)

        val currentRouteStep
            get() = route[routeStepIndex]
    }
}