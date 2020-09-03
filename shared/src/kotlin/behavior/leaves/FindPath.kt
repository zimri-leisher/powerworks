package behavior.leaves

import behavior.*
import graphics.Renderer
import graphics.TextureRenderParams
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import level.LevelManager
import level.LevelPosition
import level.entity.Entity
import main.toColor
import misc.PixelCoord
import misc.TileCoord
import serialization.Id
import java.awt.Color
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

@ExperimentalCoroutinesApi
class FindPath(parent: BehaviorTree, val goalVar: Variable, val pathDestVar: Variable, val useCoroutines: Boolean = false) : Leaf(parent) {

    override fun init(entity: Entity) {
        state = NodeState.RUNNING
        val goal = getData<LevelPosition>(goalVar)
        if (goal == null) {
            state = NodeState.SUCCESS
            return
        }
        if (useCoroutines) {
            val result = GlobalScope.async {
                route(entity, goal)
            }
            setData(DefaultVariable.PATHING_JOB, result)
            state = NodeState.RUNNING
        } else {
            val route = route(entity, goal)
            setData(pathDestVar, route)
            if (route != null) {
                state = NodeState.SUCCESS
            } else {
                state = NodeState.FAILURE
            }
        }
    }

    override fun updateState(entity: Entity): NodeState {
        if (useCoroutines) {
            val job = getData<Deferred<EntityPath?>>(DefaultVariable.PATHING_JOB)
            if (job == null) {
                return NodeState.FAILURE
            } else if (job.isActive) {
                return NodeState.RUNNING
            } else if (job.isCompleted) {
                val result = job.getCompleted()
                if (result != null) {
                    if (getData<EntityPath>(pathDestVar) == null) {
                        return NodeState.RUNNING
                    } else {
                        return NodeState.SUCCESS
                    }
                } else {
                    return NodeState.FAILURE
                }
            }
        } else {
            val route = getData<EntityPath>(pathDestVar)
            if (route != null) {
                return NodeState.SUCCESS
            } else {
                return NodeState.FAILURE
            }
        }
        return NodeState.FAILURE
    }

    override fun execute(entity: Entity) {
        if (useCoroutines) {
            val job = getData<Deferred<EntityPath?>>(DefaultVariable.PATHING_JOB)
            if (job != null) {
                if (job.isCompleted && getData<EntityPath>(pathDestVar) == null) {
                    val result = job.getCompleted()
                    setData(pathDestVar, result)
                }
            }
        }
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

data class Node(var parent: Node? = null, val pos: TileCoord, val goal: TileCoord, val entity: Entity, var g: Double) {
    var h = heuristic(this)
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
                if ((x != 0 || y != 0) && (nCoord == goal || entity.getCollisions(nXTile shl 4, nYTile shl 4).filter { it !is Entity || (it.xVel == 0.0 && it.yVel == 0.0 && it.group != entity.group) }.none())) {
                    neighbors.add(Node(this, nCoord, goal, entity, this.g + Math.sqrt(Math.pow(xTile - nXTile.toDouble(), 2.0) + Math.pow(yTile - nYTile.toDouble(), 2.0))))
                }
            }
        }
        return neighbors
    }
}

data class EntityPath(
        @Id(1) val goal: LevelPosition, @Id(2) val steps: List<PixelCoord>) {
    private constructor() : this(LevelPosition(0, 0, LevelManager.EMPTY_LEVEL), listOf())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EntityPath

        if (goal != other.goal) return false
        if (steps != other.steps) return false

        return true
    }

    override fun hashCode(): Int {
        var result = goal.hashCode()
        result = 31 * result + steps.hashCode()
        return result
    }
}

fun heuristic(node: Node): Double {
    return Math.sqrt(Math.pow(node.xTile - node.goal.xTile.toDouble(), 2.0) + Math.pow(node.yTile - node.goal.yTile.toDouble(), 2.0))
}

fun route(entity: Entity, goal: LevelPosition): EntityPath? {
    if (entity.xTile == goal.xTile && entity.yTile == goal.yTile) {
        return EntityPath(goal, listOf())
    }

    var nodeCount = 0
    var step = 0

    val startNode = Node(null, TileCoord(entity.xTile, entity.yTile), goal.tile(), entity, 0.0)

    val openNodes = mutableListOf(startNode)
    val closedNodes = mutableListOf<Node>()

    var finalNode: Node? = null

    while (openNodes.isNotEmpty()) {
        val nextNode = openNodes.minBy { it.f }!!
        if (nextNode.pos == goal.tile()) {
            finalNode = nextNode
            break
        }
        val neighbors = nextNode.getNeighbors()
        for (child in neighbors) {
            if (closedNodes.any { it.pos == child.pos }) {
                continue
            }
            nodeCount++
            val alreadyThere = openNodes.firstOrNull { it.pos == child.pos }
            if (alreadyThere != null) {
                if (alreadyThere.g < child.g) {
                    continue
                } else if (alreadyThere.g > child.g) {
                    alreadyThere.g = child.g
                    alreadyThere.parent = child.parent
                }
            } else {
                openNodes.add(child)
            }
        }
        openNodes.remove(nextNode)
        closedNodes.add(nextNode)
        step++
        if (step > 300) {
            // just to stop infinite searches
            return null
        }
        if (FindPath.renderForEntity == entity) {
            if (step >= FindPath.currentStep / 10) {
                val usedNodes = backtrack(nextNode)
                FindPath.lastPathCreated = EntityPath(goal, usedNodes.map { it.pos.pixel() })
                FindPath.openNodes = openNodes
                FindPath.usedNodes = usedNodes
                FindPath.closedNodes = closedNodes
                return null
            }
        }
    }
    if (finalNode != null) {
        val usedNodes = backtrack(finalNode)
        val path = EntityPath(goal, usedNodes.map { it.pos.pixel() })
        if (FindPath.renderForEntity == entity) {
            FindPath.lastPathCreated = path
            FindPath.openNodes = openNodes
            FindPath.usedNodes = usedNodes
            FindPath.closedNodes = closedNodes
            FindPath.currentStep = 0
        }
        return smooth(path, entity)
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

fun smooth(path: EntityPath, forEntity: Entity): EntityPath {
    if (path.steps.size == 2) {
        return path
    }
    val newPathSteps = mutableListOf<PixelCoord>()
    newPathSteps.addAll(path.steps)
    var checkPoint = path.steps[0]
    var currentPointIndex = 1
    var currentPoint = path.steps[currentPointIndex]
    while (currentPointIndex + 1 <= path.steps.lastIndex) { // while there is a next step
        if (traversable(checkPoint, path.steps[currentPointIndex + 1], forEntity)) {
            val temp = currentPoint
            currentPoint = path.steps[currentPointIndex + 1]
            newPathSteps.remove(temp)
        } else {
            checkPoint = currentPoint
            currentPoint = path.steps[currentPointIndex + 1]
        }
        currentPointIndex++
    }
    return EntityPath(path.goal, newPathSteps)
}

const val TRAVERSABLE_CHECK_STEP = 4

fun traversable(start: PixelCoord, end: PixelCoord, entity: Entity): Boolean {
    val pointsToCheck = mutableListOf<PixelCoord>()
    // generate points from the start to the end with steps between them of TRAVERSABLE_CHECK_STEP
    val angle = atan2(end.yPixel - start.yPixel.toDouble(), end.xPixel - start.xPixel.toDouble())
    var currentPoint = start
    var count = 0
    while (currentPoint.distance(end) > TRAVERSABLE_CHECK_STEP) {
        count++
        currentPoint = PixelCoord(start.xPixel + (TRAVERSABLE_CHECK_STEP * count * cos(angle)).toInt(), start.yPixel + (TRAVERSABLE_CHECK_STEP * count * sin(angle)).toInt())
        if (entity.getCollisions(currentPoint.xPixel, currentPoint.yPixel).any()) {
            return false
        }
    }
    return true
}