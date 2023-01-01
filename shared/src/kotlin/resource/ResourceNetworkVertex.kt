package resource

import level.Level
import level.LevelObject
import serialization.AsReference
import serialization.Id
import serialization.Referencable

interface PotentialResourceNetworkVertex<T : ResourceNetworkVertex<T>> : Referencable<LevelObject> {
    val x: Int
    val y: Int
    val level: Level
    val inLevel: Boolean
    var vertex: T?
    val networks: Set<ResourceNetwork<*>>

    fun getNetwork(type: ResourceNetworkType): ResourceNetwork<*>?
    fun onAddToNetwork(network: ResourceNetwork<*>)
    fun onRemoveFromNetwork(network: ResourceNetwork<*>)
}

abstract class ResourceNetworkVertex<T : ResourceNetworkVertex<T>>(
    @Id(1)
    @AsReference
    val obj: PotentialResourceNetworkVertex<T>,
    @Id(2)
    val edges: MutableList<T?>,
    @Id(3)
    val type: ResourceNetworkType
)