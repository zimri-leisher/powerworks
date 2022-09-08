package resource

import graphics.Renderer
import graphics.TextureRenderParams
import level.Level
import level.getBlockAtTile
import level.getResourceNodeAt
import level.pipe.PipeBlock
import main.toColor
import misc.Geometry
import misc.TileCoord
import java.lang.Integer.max
import java.lang.Integer.min
import java.util.*


class PipeNetwork(level: Level, vertices: Set<PipeNetworkVertex> = setOf()) : ResourceNetwork(level) {

    val vertices = vertices.toMutableSet()

    val pipes = mutableListOf<PipeBlock>()
    val connections = mutableListOf<PipeNetworkConnection>()
    override val nodes = mutableListOf<ResourceNode2>()

    init {
        for (vert in vertices) {
            if (vert is PipeBlock) {
                pipes.add(vert)
            } else {
                nodes.add(vert as ResourceNode2)
            }
        }
    }

    override fun getConnection(from: ResourceContainer, to: ResourceContainer): ResourceNodeConnection? {
        val fromNodes = from

        if (from !in nodes || to !in nodes) {
            return null
        }
        val steps = route(from, to) ?: return null
        val connection = PipeNetworkConnection(this, steps)
        connections.add(connection)
        return connection
    }

    fun updateNearConnections(vert: PipeNetworkVertex) {
        for (dir in 0..3) {
            val nearVert = findNearVertex(vert, dir)
            if (vert.inLevel) {
                vert.nearEdges[dir] = nearVert
                nearVert?.nearEdges?.set(Geometry.getOppositeAngle(dir), vert)
            } else {
                vert.nearEdges[dir] = null
                nearVert?.nearEdges?.set(Geometry.getOppositeAngle(dir), null)
            }
            (nearVert as? PipeBlock)?.updateState()
        }
        (vert as? PipeBlock)?.updateState()
    }

    fun updateFarConnections(vert: PipeNetworkVertex) {
        if (vert.inLevel && vert.validFarVertex) {
            for (dir in 0..3) {
                vert.farEdges[dir] = findFarVertex(vert, dir)
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
                val farVert = findFarVertex(vert, dir)
                farVert?.farEdges?.set(Geometry.getOppositeAngle(dir), null)
                vert.farEdges[dir] = null
            }
        }
    }

    private fun updateFarConnectionsRecurse(vert: PipeNetworkVertex) {
        updateFarConnections(vert)
        for (dir in 0..3) {
            val farVert = findFarVertex(vert, dir)
            val nearVert = findNearVertex(vert, dir)
            if (farVert == nearVert) {
                farVert?.let { updateFarConnections(it) }
            } else {
                farVert?.let { updateFarConnections(it) }
                nearVert?.let { updateFarConnections(it) }
            }
        }
    }

    private fun updateConnections(vert: PipeNetworkVertex) {
        updateNearConnections(vert)
        updateFarConnectionsRecurse(vert)
    }

    fun add(vert: PipeNetworkVertex) {
        // add to internal lists
        vertices.add(vert)
        if (vert is PipeBlock) {
            pipes.add(vert)
        } else {
            nodes.add(vert as ResourceNode2)
        }
        updateConnections(vert)
        tryMerge(vert)
    }

    fun remove(vert: PipeNetworkVertex) {
        updateConnections(vert)
        trySplit(vert)
        vertices.remove(vert)
        if (vert is PipeBlock) {
            pipes.remove(vert)
        } else {
            nodes.remove(vert as ResourceNode2)
        }
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

    private fun route(from: ResourceNode2, to: ResourceNode2): List<PipeNetworkVertex>? {

        fun h(src: PipeNetworkVertex): Double {
            return Geometry.distance(src.xTile * 16, src.yTile * 16, to.x, to.y)
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
                while(current != null) {
                    path.add(current.vert)
                    current = current.parent
                }
                return path.reversed()
            }
            for(neighbor in next.vert.farEdges) {
                if(neighbor == null) {
                    continue
                }
                val oldG = g[TileCoord(next.vert.xTile, next.vert.yTile)] ?: Double.POSITIVE_INFINITY
                val newG = oldG + Geometry.distance(neighbor.xTile * 16, neighbor.yTile * 16, next.vert.xTile * 16, next.vert.yTile * 16)
                if(newG < (g[TileCoord(neighbor.xTile, neighbor.yTile)] ?: Double.POSITIVE_INFINITY)) {
                    val newNode = PipeNetworkRoutingNode(neighbor, next, newG + h(neighbor))
                    g[TileCoord(neighbor.xTile, neighbor.yTile)] = newG
                    if(newNode !in open) {
                        open.add(newNode)
                    }
                }
            }
        }
        return null
    }

    fun mergeFrom(network: PipeNetwork) {
        for (vertex in network.vertices) {
            vertex.network = this
        }
        vertices.addAll(network.vertices)
        pipes.addAll(network.pipes)
        nodes.addAll(network.nodes)
        network.vertices.clear()
        network.pipes.clear()
        network.nodes.clear()
    }

    private fun tryMerge(around: PipeNetworkVertex) {
        val toCombine = mutableSetOf(this)
        for (dir in 0..3) {
            if (around.nearEdges[dir]?.network != null && around.nearEdges[dir]?.network != this) {
                toCombine.add(around.nearEdges[dir]!!.network!!)
            }
        }
        merge(toCombine)
    }

    private fun trySplit(around: PipeNetworkVertex) {
        val found = Array<MutableSet<PipeNetworkVertex>?>(4) { null }
        for (dir in 0..3) {
            val start = findNearVertex(around, dir)
            if (start != null) {
                found[dir] = mutableSetOf()
                bfs(start,
                    getChildren = { it.nearEdges },
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

    private fun findNearVertex(vert: PipeNetworkVertex, dir: Int): PipeNetworkVertex? {
        val x = vert.xTile + Geometry.getXSign(dir)
        val y = vert.yTile + Geometry.getYSign(dir)
        return level.getBlockAtTile(x, y) as? PipeBlock ?: level.getResourceNodeAt(x, y)
    }

    // depends on adjacent vertices being set
    private fun findFarVertex(vert: PipeNetworkVertex, dir: Int): PipeNetworkVertex? {
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
        val rand = Random(id.toLong())
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

    override fun equals(other: Any?): Boolean {
        return other is PipeNetwork && other.id == this.id
    }

    override fun toString(): String {
        return "PipeNetwork(id=$id, size=${vertices.size}, level=${level.id})"
    }

    override fun hashCode(): Int {
        return id
    }

    companion object {

        fun merge(networks: Set<PipeNetwork>) {
            if (networks.isEmpty()) {
                return
            }
            val dest = networks.maxBy { it.vertices.size }
            for (other in networks - dest) {
                dest.mergeFrom(other)
            }
        }

        fun split(network: PipeNetwork, groups: Set<Set<PipeNetworkVertex>>) {
            val largestGroup = groups.withIndex().maxBy { (_, value) -> value.size }.index
            for ((i, newVertices) in groups.withIndex()) {
                if (i != largestGroup) {
                    val newNetwork = PipeNetwork(network.level, newVertices)
                    for (vert in newVertices) {
                        vert.network = newNetwork
                        network.vertices.remove(vert)
                        if (vert is PipeBlock) {
                            network.pipes.remove(vert)
                        } else {
                            network.nodes.remove(vert as ResourceNode2)
                        }
                    }
                }
            }
        }
    }
}