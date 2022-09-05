package routing

import data.ConcurrentlyModifiableMutableList
import graphics.Image
import graphics.Renderer
import graphics.TextureRenderParams
import graphics.text.TextManager
import graphics.text.TextRenderParams
import item.Inventory
import item.ItemType
import level.Level
import level.LevelManager
import level.update.ResourceNodeTransferThrough
import level.pipe.PipeBlock
import main.DebugCode
import main.Game
import misc.Geometry
import misc.Coord
import network.ResourceNodeReference
import resource.*
import serialization.Id
import java.util.*
import kotlin.math.absoluteValue

abstract class PipeNetwork(resourceCategory: ResourceCategory, level: Level, private val speed: Int = 1) : ResourceRoutingNetwork(resourceCategory, level) {

//    private constructor() : this(ResourceCategory.ITEM, LevelManager.EMPTY_LEVEL)
//
//    @Id(12)
//    val pipes = mutableSetOf<PipeBlock>()
//
//    /**
//     * The list of [Intersection]s in this network
//     */
//    @Id(13)
//    val intersections = mutableSetOf<Intersection>()
//
//    @Id(11)
//    private val packages = ConcurrentlyModifiableMutableList<PipeRoutingPackage>()
//
//    val size
//        get() = pipes.size
//
//    fun addPipe(pipe: PipeBlock) {
//        pipes.add(pipe)
//    }
//
//    fun removePipe(pipe: PipeBlock) {
//        pipes.remove(pipe)
//        val toRemove = internalNodes.filter { it.xTile == pipe.xTile && it.yTile == pipe.yTile }
//        toRemove.forEach { level.remove(it); internalNodes.remove(it) }
//        packages.startTraversing()
//        for (pack in packages) {
//            if (Geometry.intersects(pipe.x, pipe.y, 16, 16, pack.position.x, pack.position.y + 5, 12, 12)) {
//                // if the package was partially on this tube
//                // drop it and remove it from the network
//                packages.remove(pack)
//                pack.to.attachedContainer.cancelExpectation(pack.type, pack.quantity)
//            }
//        }
//        packages.endTraversing()
//
//        val adjacentPipes = pipes.filter { (it.xTile - pipe.xTile).absoluteValue + (it.yTile - pipe.yTile).absoluteValue == 1 }
//        if (adjacentPipes.size > 1) {
//            // if there was only 1 or 0 pipe this can't possibly change the network
//            val intersection = getIntersection(pipe)
//            if (intersection != null) {
//                // if this has 2 connections it
//            }
//        }
////        if (pipe.intersection != null) {
////            removeIntersection(pipe.intersection!!)
////        }
//    }
//
//    fun getIntersection(t: PipeBlock): Intersection? {
//        return intersections.firstOrNull { it.pipeBlock == t }
//    }
//
//    fun getPipeAtOrNull(xTile: Int, yTile: Int) = pipes.firstOrNull { it.xTile == xTile && it.yTile == yTile }
//
//    override fun mergeIntoThis(other: ResourceRoutingNetwork) {
//        super.mergeIntoThis(other)
//        if (other is PipeNetwork) {
//            for (pipe in other.pipes) {
////                pipe.network = this
//                pipes.add(pipe)
//            }
//            intersections.addAll(other.intersections)
//            other.packages.forEach { packages.add(it) }
//        }
//    }
//
//    /**
//     * Updates the connections of the given intersection
//     */
//    fun updateIntersection(pipe: PipeBlock) {
//        var intersection = intersections.firstOrNull { it.pipeBlock == pipe }
//        if (intersection == null) {
//            intersection = Intersection(pipe, Connections())
//            intersections.add(intersection)
//        }
//        intersection.connections = findConnections(intersection)
//    }
//
//    fun removeIntersection(intersection: Intersection) {
//        intersections.remove(intersection)
//        for (i in 0..3) {
//            val connected = intersection.connections.intersections[i]
//            if (connected != null) {
//                connected.connections = findConnections(connected)
//            }
//        }
//    }
//
//    /**
//     * @return the [Connections] object describing the intersections in each direction and their distance to this [pipe].
//     * This will also create new intersections as needed (if there should be one but isn't)
//     */
//    fun findConnectionsWithPipe(pipe: PipeBlock): Connections {
//        val connecIntersections = arrayOfNulls<Intersection>(4)
//        val connecDists = arrayOfNulls<Int>(4)
//        for (i in 0..3) {
//            var currentPipe: PipeBlock? = null
//            var dist = 1
////            // Iterate down the pipe until there are no more connections
////            while (currentPipe != null && !currentPipe.shouldBeVertex()) {
////                currentPipe = currentPipe.pipeConnections[i]
////                dist++
////            }
////            // currentPipe is either null or an intersection
////            if (currentPipe != pipe && currentPipe != null) {
////                connecIntersections[i] = getIntersection(currentPipe)
////                connecDists[i] = dist
////            }
//        }
//        return Connections(connecIntersections, connecDists)
//    }
//
//    /**
//     * @return the [Connections] object describing the intersections in each direction and their distance to this [intersection].
//     * This will create new intersections as needed (if there should be one but isn't), and it will update the intersections
//     * this is connected to so that they are current relative to this one.
//     */
//    fun findConnections(intersection: Intersection): Connections {
//        val connecIntersections = arrayOfNulls<Intersection>(4)
//        val connecDists = arrayOfNulls<Int>(4)
//        for (i in 0..3) {
////            var currentPipe = intersection.pipeBlock.pipeConnections[i]
////            var dist = 1
////            // Iterate down the tube until there are no more connections
////            while (currentPipe != null && !currentPipe.shouldBeVertex()) {
////                currentPipe = currentPipe.pipeConnections[i]
////                dist++
////            }
////            // currentPipe is either null or an intersection
////            if (currentPipe != intersection.pipeBlock && currentPipe != null) {
////                var newIntersection = getIntersection(currentPipe)
////                if (newIntersection == null) {
////                    updateIntersection(currentPipe)
////                    newIntersection = getIntersection(currentPipe)!!
////                }
////                newIntersection.connections.distances[Geometry.getOppositeAngle(i)] = dist
////                newIntersection.connections.intersections[Geometry.getOppositeAngle(i)] = intersection
////                connecIntersections[i] = newIntersection
////                connecDists[i] = dist
////            }
//        }
//        return Connections(connecIntersections, connecDists)
//    }
//
//    override fun transferResources(type: ResourceType, quantity: Int, from: ResourceNode, to: ResourceNode): Boolean {
//        println("from $from ${from.isInternalNetworkNode} to $to ${to.isInternalNetworkNode} and ${to.attachedNode!!.isInternalNetworkNode}")
//        val route = route(from, to, this)
//        if (route != null) {
//            val expectSuccess = to.attachedNode!!.attachedContainer.expect(type, quantity)
//            if (!expectSuccess) {
//                throw Exception("Wasn't able to expect resources in ${to.attachedNode!!.attachedContainer} successfully, but was still told to transfer resources there")
//            }
//            containersSentTo.add(to.attachedNode!!.attachedContainer)
//            if (containersSentTo.size == attachedNodes.filter { it.behavior.allowIn.possible() != null }.map { it.attachedContainer }.distinct().size) {
//                // if we've sent to all internal containers
//                containersSentTo.clear()
//            }
//            packages.add(PipeRoutingPackage(from, to, type, quantity, route))
//            return true
//        }
//        return false
//    }
//
//    private fun reroutePackage(pack: PipeRoutingPackage) {
//        val newDestination = findDestinationFor(pack.type, pack.quantity)
//        if (newDestination != null) {
//            val route = route(pack.position.x, pack.position.y, newDestination, this)
//            if (route != null) {
//                if (newDestination.attachedNode!!.attachedContainer.expect(pack.type, pack.quantity)) {
//                    println("routing resource: $route")
//                    pack.from = pack.to
//                    pack.to.attachedContainer.cancelExpectation(pack.type, pack.quantity)
//                    pack.to = newDestination
//                    pack.route = route
//                    //if the package should be moving on the x axis, look for the nearest step in the same y
//                    pack.routeStepIndex = 0
//                    pack.dir = Geometry.getDir(route[0].position.x - pack.position.x, route[0].position.y - pack.position.y)
//                    println("${(route[0].position.x shr 4) - (pack.position.x shr 4)} ${(route[0].position.y shr 4) - (pack.position.y shr 4)}")
//                    println("${pack.dir}")
//                    pack.awaitingRoute = false
//                }
//            }
//        } else {
//            // no dest available
//            pack.awaitingRoute = true
//        }
//    }
//
//    override fun update() {
//
//        fun atDestination(p: PipeRoutingPackage): Boolean {
//            if (p.position.manhattanDistance(p.route[p.route.lastIndex].position) <= speed) {
//                return true
//            }
//            return false
//        }
//        packages.forEach { pack ->
//
//            if (pack.awaitingRoute || pack.to.attachedNode == null || !pack.to.attachedNode!!.canInput(pack.type, pack.quantity)) {
//                reroutePackage(pack)
//            } else if (atDestination(pack)) {
//                level.modify(ResourceNodeTransferThrough(ResourceNodeReference(pack.to.attachedNode!!),
//                        resourceListOf(pack.type to pack.quantity), false, false, true))
//                packages.remove(pack)
//            } else {
//                if (pack.position.manhattanDistance(pack.currentRouteStep.position) < speed) {
//                    pack.dir = pack.currentRouteStep.dir
//                    pack.routeStepIndex++
//                }
//                pack.position = Coord(pack.position.x + Geometry.getXSign(pack.dir) * speed, pack.position.y + Geometry.getYSign(pack.dir) * speed)
//            }
//        }
//    }
//
//    override fun render() {
//        packages.forEach {
//            it.type.icon.render(it.position.x, it.position.y + 5, 12, 12, true)
//            Renderer.renderText(it.quantity, it.position.x + 2, it.position.y + 7, TextRenderParams(size = TextManager.DEFAULT_SIZE - 5))
//            if (Game.currentDebugCode == DebugCode.PIPE_INFO) {
//                val xDistance = it.currentRouteStep.position.x - it.position.x
//                val yDistance = it.currentRouteStep.position.y - it.position.y
//                Renderer.renderFilledRectangle(it.position.x, it.position.y + 5, if (xDistance == 0) 2 else xDistance, if (yDistance == 0) 2 else yDistance)
//                for (index in (it.routeStepIndex + 1)..it.route.lastIndex) {
//                    val step = it.route[index]
//                    val lastStep = it.route[index - 1]
//                    val xDistanceStep = step.position.x - lastStep.position.x
//                    val yDistanceStep = step.position.y - lastStep.position.y
//                    Renderer.renderFilledRectangle(lastStep.position.x, lastStep.position.y + 5, if (xDistanceStep == 0) 2 else xDistanceStep, if (yDistanceStep == 0) 2 else yDistanceStep)
//                    Renderer.renderTexture(Image.Misc.THIN_ARROW, lastStep.position.x, lastStep.position.y, 8, 8, TextureRenderParams(rotation = Geometry.getDegrees(lastStep.dir + 1)))
//                }
//            }
//        }
//    }
//
//    data class PipeRoutingPackage(
//            @Id(1)
//            var from: ResourceNode,
//            @Id(2)
//            var to: ResourceNode,
//            @Id(3)
//            var type: ResourceType,
//            @Id(4)
//            var quantity: Int,
//            @Id(5)
//            var route: PackageRoute,
//            @Id(6)
//            var routeStepIndex: Int = 0,
//            @Id(7)
//            var position: Coord = Coord(route[0].position.x, route[0].position.y),
//            @Id(8)
//            var dir: Int = route[0].dir,
//            @Id(9)
//            var awaitingRoute: Boolean = false) {
//
//
//        @Id(10)
//        val id = UUID.randomUUID()!!
//
//        private constructor() : this(ResourceNode(0, 0, 0, ResourceCategory.ITEM, Inventory(0, 0), LevelManager.EMPTY_LEVEL),
//                ResourceNode(0, 0, 0, ResourceCategory.ITEM, Inventory(0, 0), LevelManager.EMPTY_LEVEL), ItemType.ERROR, 0, PackageRoute(arrayOf()), 0, Coord(0, 0), 0)
//
//        val currentRouteStep
//            get() = route[routeStepIndex]
//
//        override fun equals(other: Any?): Boolean {
//            if (this === other) return true
//            if (javaClass != other?.javaClass) return false
//
//            other as PipeRoutingPackage
//
//            if (id != other.id) return false
//
//            return true
//        }
//
//        override fun hashCode(): Int {
//            return id.hashCode()
//        }
//    }
}