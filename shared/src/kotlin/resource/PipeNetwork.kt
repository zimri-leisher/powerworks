package resource

import graphics.Renderer
import graphics.TextureRenderParams
import level.*
import level.pipe.PipeBlock
import main.toColor
import misc.Geometry
import misc.TileCoord
import network.LevelObjectReference
import network.ResourceNetworkReference
import java.lang.Integer.max
import java.lang.Integer.min
import java.util.*

interface PotentialPipeNetworkVertex : PotentialResourceNetworkVertex {
    val validFarVertex: Boolean
    var vertex: PipeNetworkVertex?
}

class PipeNetworkVertex(
    obj: PotentialPipeNetworkVertex,
    edges: MutableList<PipeNetworkVertex?>,
    val farEdges: Array<PipeNetworkVertex?>
) :
    ResourceNetworkVertex<PipeNetworkVertex>(obj, edges, ResourceNetworkType.PIPE) {

    private val pipeObj get() = obj as PotentialPipeNetworkVertex

    val xTile get() = obj.x / 16
    val yTile get() = obj.y / 16

    val level get() = obj.level

    val inLevel get() = obj.inLevel

    val validFarVertex get() = pipeObj.validFarVertex
}

class PipeNetwork(level: Level) : ResourceNetwork<PipeNetworkVertex>(level, ResourceNetworkType.PIPE) {

    val connections = mutableListOf<PipeNetworkConnection>()
    override val nodes = mutableListOf<ResourceNode>()

    override fun canBeVertex(obj: PhysicalLevelObject): Boolean {
        return obj is PotentialPipeNetworkVertex
    }

    override fun makeVertex(obj: PhysicalLevelObject): PipeNetworkVertex {
        return PipeNetworkVertex(obj as PotentialPipeNetworkVertex, mutableListOf(), arrayOfNulls(4))
    }

    override fun getConnection(from: ResourceNode2, to: ResourceNode2): ResourceNodeConnection? {
        if (from !in nodes || to !in nodes) {
            return null
        }
        val existingConnection = connections.firstOrNull { it.from == from && it.to == to }
        if (existingConnection != null) {
            return existingConnection
        }
        val steps = route(from, to) ?: return null
        val connection = PipeNetworkConnection(this, steps)
        connections.add(connection)
        return connection
    }

    fun updateNearConnections(vert: PipeNetworkVertex) {
        for (dir in 0..3) {
            val nearVert = tryMakeNearVertex(vert, dir)
            if (vert.inLevel) {
                vert.edges[dir] = nearVert
                nearVert?.edges?.set(Geometry.getOppositeAngle(dir), vert)
            } else {
                vert.edges[dir] = null
                nearVert?.edges?.set(Geometry.getOppositeAngle(dir), null)
            }
            (nearVert?.obj as? PipeBlock)?.updateState()
        }
        (vert.obj as? PipeBlock)?.updateState()
    }

    fun updateFarConnections(vert: PipeNetworkVertex) {
        if (vert.inLevel && vert.validFarVertex) {
            for (dir in 0..3) {
                val potentialVert = tryMakeFarVertex(vert, dir)
                vert.farEdges[dir] = potentialVert
                vert.farEdges[dir]?.farEdges?.set(Geometry.getOppositeAngle(dir), vert)
            }
        } else if (vert.inLevel) {
            // can still use this as passthru
            for (axis in 0..1) {
                val plusDir = axis
                val minusDir = axis + 2
                val plus = vert.farEdges[plusDir]
                val minus = vert.farEdges[minusDir]
                plus?.farEdges?.set(minusDir, minus)
                minus?.farEdges?.set(plusDir, plus)
                vert.farEdges[plusDir] = null
                vert.farEdges[minusDir] = null
            }
        } else {
            for (dir in 0..3) {
                val farVert = tryMakeFarVertex(vert, dir)
                farVert?.farEdges?.set(Geometry.getOppositeAngle(dir), null)
                vert.farEdges[dir] = null
            }
        }
    }

    private fun updateFarConnectionsRecurse(vert: PipeNetworkVertex) {
        updateFarConnections(vert)
        for (dir in 0..3) {
            val farVert = tryMakeFarVertex(vert, dir)
            val nearVert = tryMakeNearVertex(vert, dir)
            if (farVert == nearVert) {
                farVert?.let { updateFarConnections(it) }
            } else {
                farVert?.let { updateFarConnections(it) }
                nearVert?.let { updateFarConnections(it) }
            }
        }
    }

    override fun updateEdges(vert: PipeNetworkVertex) {
        updateNearConnections(vert)
        updateFarConnectionsRecurse(vert)
    }

    override fun trySplit(around: PipeNetworkVertex) {
        val found = Array<MutableSet<PipeNetworkVertex>?>(4) { null }
        for (dir in 0..3) {
            val start = around.edges[dir]
            if (start != null) {
                found[dir] = mutableSetOf()
                bfs(start,
                    getChildren = { it.edges.toTypedArray() },
                    processVertex = {
                        found[dir]!!.add(it)
                    })
            }
        }
        if (found.count { it != null } <= 1) {
            // no need to split
            return
        }
        val groups = mutableSetOf<MutableSet<PipeNetworkVertex>>()
        outer@ for (dir in 0..3) {
            if (found[dir] == null) {
                continue
            }
            for (otherDir in 0 until dir) {
                if (found[otherDir] == null) {
                    continue
                }
                if (found[dir]!!.first() in found[otherDir]!!) {
                    // if the group at dir and otherDir have a non empty intersection, they will be the same
                    // let's ignore the group at dir for now, cuz this means it will be added later
                    continue@outer
                } // otherwise the groups are distinct, go check the next one
            }
            // if we made it all the way through then this group is distinct from the others
            groups.add(found[dir]!!)
        }
        if (groups.size == 1) {
            // no need to split
            return
        }
        split(this, groups)
    }

    override fun splitOff(vertices: Collection<PipeNetworkVertex>): ResourceNetwork<PipeNetworkVertex> {
        val newNetwork = PipeNetwork(level)
        newNetwork.vertices.addAll(vertices)
        for (vert in vertices) {
            vert.obj.onRemoveFromNetwork(this)
            vert.obj.onAddToNetwork(newNetwork)
            this.vertices.remove(vert)
            if (vert.obj is ResourceNode) {
                this.nodes.remove(vert.obj)
            }
        }
        return newNetwork
    }

    private data class PipeNetworkRoutingNode(
        val vert: PipeNetworkVertex,
        val parent: PipeNetworkRoutingNode?,
        val f: Double
    ) : Comparable<PipeNetworkRoutingNode> {
        override fun compareTo(other: PipeNetworkRoutingNode): Int {
            return f.compareTo(other.f)
        }
    }

    private fun route(from: PipeNetworkVertex, to: PipeNetworkVertex): List<PipeNetworkVertex>? {

        fun h(src: PipeNetworkVertex): Double {
            return Geometry.distance(src.xTile * 16, src.yTile * 16, to.xTile * 16, to.yTile * 16)
        }

        val start = PipeNetworkRoutingNode(from, null, h(from))
        val open = PriorityQueue<PipeNetworkRoutingNode>()
        open.add(start)
        val g = mutableMapOf<TileCoord, Double>()
        g[TileCoord(from.xTile, from.yTile)] = 0.0
        while (open.isNotEmpty()) {
            val next: PipeNetworkRoutingNode = open.poll()
            if (next.vert == to) {
                val path = mutableListOf<PipeNetworkVertex>()
                var current: PipeNetworkRoutingNode? = next
                while (current != null) {
                    path.add(current.vert)
                    current = current.parent
                }
                return path.reversed()
            }
            for (neighbor in next.vert.farEdges) {
                if (neighbor == null) {
                    continue
                }
                val oldG = g[TileCoord(next.vert.xTile, next.vert.yTile)] ?: Double.POSITIVE_INFINITY
                val newG = oldG + Geometry.distance(
                    neighbor.xTile * 16,
                    neighbor.yTile * 16,
                    next.vert.xTile * 16,
                    next.vert.yTile * 16
                )
                if (newG < (g[TileCoord(neighbor.xTile, neighbor.yTile)] ?: Double.POSITIVE_INFINITY)) {
                    val newNode = PipeNetworkRoutingNode(neighbor, next, newG + h(neighbor))
                    g[TileCoord(neighbor.xTile, neighbor.yTile)] = newG
                    if (newNode !in open) {
                        open.add(newNode)
                    }
                }
            }
        }
        return null
    }

    private fun tryMakeNearVertex(vert: PipeNetworkVertex, dir: Int): PipeNetworkVertex? {
        return findPotentialNearVertex(vert, dir)?.let { makeVertex(it as PhysicalLevelObject) }
    }

    private fun findPotentialNearVertex(vert: PipeNetworkVertex, dir: Int): PotentialPipeNetworkVertex? {
        val x = vert.xTile + Geometry.getXSign(dir)
        val y = vert.yTile + Geometry.getYSign(dir)
        return level.getBlockAtTile(x, y) as? PipeBlock ?: level.getResourceNodeAt(x, y)
    }

    private fun tryMakeFarVertex(vert: PipeNetworkVertex, dir: Int): PipeNetworkVertex? {
        return findPotentialFarVertex(vert, dir)?.let { makeVertex(it as PhysicalLevelObject) }
    }

    // depends on adjacent vertices being set
    private fun findPotentialFarVertex(vert: PipeNetworkVertex, dir: Int): PotentialPipeNetworkVertex? {
        var x = vert.xTile
        var y = vert.yTile
        while (true) {
            x += Geometry.getXSign(dir)
            y += Geometry.getYSign(dir)
            val block = vert.level.getBlockAtTile(x, y)
            if (block is PipeBlock && block.validFarVertex) {
                return block
            } else if (block == null) {
                break
            }
        }
        val resourceNode = vert.level.getResourceNodeAt(x, y)
        if (resourceNode?.validFarVertex == true) {
            return resourceNode
        }
        return null
    }

    private fun bfs(
        start: PipeNetworkVertex,
        getChildren: (parent: PipeNetworkVertex) -> Array<PipeNetworkVertex?> = { it.farEdges },
        processEdge: (parent: PipeNetworkVertex, child: PipeNetworkVertex) -> Unit = { _, _ -> },
        processVertex: (vert: PipeNetworkVertex) -> Unit = { }
    ) {
        val open = mutableListOf(start)
        val closed = mutableListOf<PipeNetworkVertex>()
        while (open.isNotEmpty()) {
            val next = open.last()
            processVertex(next)
            closed.add(next)
            open.removeLast()
            for (neighbor in getChildren(next).filterNotNull()) {
                if (neighbor !in closed) {
                    open.add(neighbor)
                    processEdge(next, neighbor)
                }
            }
        }
    }

    override fun render() {
        val start = vertices.firstOrNull { it.validFarVertex } ?: return
        val rand = Random(id.leastSignificantBits)
        val color = toColor(rand.nextInt(255), rand.nextInt(255), rand.nextInt(255))
        bfs(start, processEdge = { parent, child ->
            val x1 = parent.xTile * 16 + 8
            val y1 = parent.yTile * 16 + 8
            val x2 = child.xTile * 16 + 8
            val y2 = child.yTile * 16 + 8
            val startX = min(x1, x2)
            val startY = min(y1, y2)
            val width = max(kotlin.math.abs(x2 - x1), 4)
            val height = max(kotlin.math.abs(y2 - y1), 4)
            Renderer.renderFilledRectangle(startX, startY, width, height, TextureRenderParams(color = color))
        })
    }

    override fun toReference(): LevelObjectReference {
        return ResourceNetworkReference(this)
    }

    override fun equals(other: Any?): Boolean {
        return other is PipeNetwork && other.id == this.id
    }

    override fun toString(): String {
        return "PipeNetwork(id=$id, size=${vertices.size}, level=${level.id})"
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }


}