package behavior.leaves

import behavior.BehaviorTree
import behavior.DataLeaf
import behavior.Variable
import graphics.Renderer
import graphics.TextureRenderParams
import level.entity.Entity
import main.toColor
import misc.PixelCoord
import misc.TileCoord
import java.awt.Color
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sqrt

class FindPath(parent: BehaviorTree, val goalVar: Variable, val pathDestVar: Variable, val useCoroutines: Boolean = false) : DataLeaf(parent) {

    var currentPathingQueue: MutableMap<Entity, TileCoord>? = null

    override fun run(entity: Entity): Boolean {
        val goalAny = getData<Any?>(goalVar)
        val goal: TileCoord
        if (goalAny is PixelCoord) {
            goal = goalAny.toTile()
        } else {
            goal = goalAny as TileCoord
        }
        if(useCoroutines) {
            if(currentPathingQueue != null) {
                currentPathingQueue!!.put(entity, goal)
            } else {
                currentPathingQueue = mutableMapOf(entity to goal)
            }
            
        }
        val route = route(entity, goal)
        setData(pathDestVar, route)
        return route != null
    }

    override fun toString() = "FindPath: (goalVar: $goalVar, pathDestVar: $pathDestVar)"

    companion object {
        var lastPathCreated: EntityPath? = null
        var openNodes: List<Node>? = null
        var usedNodes: List<Node>? = null
        var closedNodes: List<Node>? = null
        var goal: TileCoord? = null
        var currentStep = 0

        var renderForEntity: Entity? = null

        fun setRenderPathfinding(entity: Entity) {
            renderForEntity = entity
        }

        fun render() {
            if (renderForEntity != null) {
                if (lastPathCreated != null && openNodes != null && usedNodes != null && closedNodes != null) {
                    for (node in closedNodes!!) {
                        Renderer.renderFilledRectangle(node.pos.xTile shl 4, node.pos.yTile shl 4, 16, 16)
                    }
                    for (node in usedNodes!!) {
                        Renderer.renderFilledRectangle(node.pos.xTile shl 4, node.pos.yTile shl 4, 16, 16, params = TextureRenderParams(color = toColor(r = 1.0f, g = 0.0f, b = 0.0f)))
                    }
                    for (node in openNodes!!) {
                        val max = openNodes!!.maxBy { it.f }!!.f
                        val min = openNodes!!.minBy { it.f }!!.f
                        var hue = (node.f - min) / (max - min)
                        hue = 1 - floor(hue * 10) / 10
                        //Renderer.renderText(hue, node.pos.xTile shl 4, node.pos.yTile shl 4)
                        Renderer.renderEmptyRectangle(node.pos.xTile shl 4, node.pos.yTile shl 4, 16, 16, params = TextureRenderParams(color = toColor(Color.HSBtoRGB(hue.toFloat(), 1f, 1f))))
                    }
                    currentStep++
                }
            }
        }
    }
}

data class Node(var parent: Node? = null, val pos: TileCoord, val goal: TileCoord, val entity: Entity) {
    var g = 0.0
    var h = 0.0
    val f get() = g + h
    val xTile get() = pos.xTile
    val yTile get() = pos.yTile

    fun getNeighbors(): List<Node> {
        val neighbors = mutableListOf<Node>()
        for (x in -1..1) {
            for (y in -1..1) {
                val nXTile = xTile + x
                val nYTile = yTile + y
                val nCoord = TileCoord(nXTile, nYTile)
                if ((x != 0 || y != 0) && (nCoord == goal || entity.getCollision(nXTile shl 4, nYTile shl 4, { it !is Entity }) == null)) {
                    neighbors.add(Node(null, nCoord, goal, entity))
                }
            }
        }
        return neighbors
    }
}

data class EntityPath(val goal: PixelCoord, val steps: List<PixelCoord>)

fun moveCost(node1: Node, node2: Node): Double {
    val dX = node1.xTile - node2.xTile.toDouble()
    val dY = node1.yTile - node2.yTile.toDouble()
    return Math.sqrt(Math.pow(dX, 2.0) + Math.pow(dY, 2.0))
}

fun heuristic(node: Node): Double {
    return Math.sqrt(Math.pow(node.xTile - node.goal.xTile.toDouble(), 2.0) + Math.pow(node.yTile - node.goal.yTile.toDouble(), 2.0))
}

fun costFromStart(node: Node): Double = if (node.parent == null) 0.0 else (node.parent!!.g + moveCost(node.parent!!, node))

fun route(entity: Entity, goal: TileCoord): EntityPath? {
    if (entity.xTile == goal.xTile && entity.yTile == goal.yTile) {
        return EntityPath(goal.toPixel(), listOf())
    }
    var nodeCount = 0
    var step = 0
    val startNode = Node(null, TileCoord(entity.xTile, entity.yTile), goal, entity)
    var openNodes = mutableListOf(startNode)
    val closedNodes = mutableListOf<Node>()
    var finalNode: Node? = null
    if (FindPath.renderForEntity == entity && FindPath.goal != goal) {
        FindPath.goal = goal
        FindPath.currentStep = 0
    }
    main@ while (openNodes.isNotEmpty()) {
        val nextNode = openNodes.minBy { it.f }!!
        closedNodes.add(nextNode)
        openNodes.remove(nextNode)
        val neighbors = nextNode.getNeighbors()
        nodeCount += neighbors.size
        for (child in neighbors) {
            if (closedNodes.any { it.pos == child.pos }) {
                continue
            }
            child.parent = nextNode
            if (child.pos == goal) {
                finalNode = child
                break@main
            }
            child.g = costFromStart(child)
            child.h = heuristic(child)
            val alreadyThere = openNodes.firstOrNull { it.pos == child.pos }
            if (alreadyThere != null) {
                if (alreadyThere.g < child.g) {
                    continue
                } else {
                    openNodes.remove(alreadyThere)
                }
            }
            openNodes = (listOf(child) + openNodes).toMutableList()
        }
        step++
        if (FindPath.renderForEntity == entity) {
            if (step >= FindPath.currentStep / 10) {
                val usedNodes = backtrack(nextNode)
                FindPath.lastPathCreated = EntityPath(goal.toPixel(), usedNodes.map { it.pos.toPixel() })
                FindPath.openNodes = openNodes
                FindPath.usedNodes = usedNodes
                FindPath.closedNodes = closedNodes
                return null
            }
        }
    }
    //println("created $nodeCount")
    if (finalNode != null) {
        val usedNodes = backtrack(finalNode)
        val path = EntityPath(goal.toPixel(), usedNodes.map { it.pos.toPixel() })
        if (FindPath.renderForEntity == entity) {
            FindPath.lastPathCreated = path
            FindPath.openNodes = openNodes
            FindPath.usedNodes = usedNodes
            FindPath.closedNodes = closedNodes
            FindPath.currentStep = 0
        }
        return path
    } else {
        return null
    }
}

fun backtrack(endingNode: Node): List<Node> {
    var currentNode: Node? = endingNode
    val pathedNodes = mutableListOf(currentNode!!)
    while (currentNode?.parent != null) {
        currentNode = currentNode.parent
        pathedNodes.add(currentNode!!)
    }
    return pathedNodes.reversed()
}