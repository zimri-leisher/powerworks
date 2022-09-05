package resource

import level.Level
import misc.Geometry

interface PipeNetworkVertex {
    val xTile: Int
    val yTile: Int
    val farEdges: Array<PipeNetworkVertex?>
    val nearEdges: Array<PipeNetworkVertex?>
    var inLevel: Boolean
    val level: Level
    val validFarVertex: Boolean
    var network: PipeNetwork?
}