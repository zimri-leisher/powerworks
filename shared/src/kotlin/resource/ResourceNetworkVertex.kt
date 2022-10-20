package resource

import level.Level
import serialization.Id

interface PotentialResourceNetworkVertex {
    val x: Int
    val y: Int
    val level: Level
    val inLevel: Boolean

    val networks: List<ResourceNetwork<*>>
    fun getNetwork(type: ResourceNetworkType): ResourceNetwork<*>?
    fun onAddToNetwork(network: ResourceNetwork<*>)
    fun onRemoveFromNetwork(network: ResourceNetwork<*>)
}

abstract class ResourceNetworkVertex<T : ResourceNetworkVertex<T>>(
    @Id(1)
    val obj: PotentialResourceNetworkVertex,
    @Id(2)
    val edges: MutableList<T?>,
    @Id(3)
    val type: ResourceNetworkType
)