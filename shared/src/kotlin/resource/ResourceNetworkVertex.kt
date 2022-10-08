package resource

import level.Level

interface PotentialResourceNetworkVertex {
    val x: Int
    val y: Int
    val level: Level
    val inLevel: Boolean
    fun getNetwork(type: ResourceNetworkType): ResourceNetwork<*>?
    fun onAddToNetwork(network: ResourceNetwork<*>)
    fun onRemoveFromNetwork(network: ResourceNetwork<*>)
}

abstract class ResourceNetworkVertex<T : ResourceNetworkVertex<T>>(val obj: PotentialResourceNetworkVertex, val edges: MutableList<T?>, val type: ResourceNetworkType) {
}

//data class PipeNetworkVertex  {
//    val xTile get() = x / 16
//    val yTile get() = y / 16
//    val farEdges: MutableList<PipeNetworkVertex?>
//    val validFarVertex: Boolean
//}