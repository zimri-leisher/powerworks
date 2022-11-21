package resource

import level.Level

enum class ResourceNetworkType(val makeNew: () -> ResourceNetwork<*>) {
    PIPE({ PipeNetwork() })
}