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

abstract class ResourceNetworkVertex<T : ResourceNetworkVertex<T>>(
    val obj: PotentialResourceNetworkVertex,
    val edges: MutableList<T?>,
    val type: ResourceNetworkType
)