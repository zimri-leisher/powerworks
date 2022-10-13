package resource

import level.Level

enum class ResourceNetworkType(val makeNew: (level: Level) -> ResourceNetwork<*>) {
    PIPE({ PipeNetwork(it) })
}