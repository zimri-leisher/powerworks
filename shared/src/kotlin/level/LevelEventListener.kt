package level

enum class LevelEvent {
    LOAD, UNLOAD, INITIALIZE, PAUSE, UNPAUSE
}

interface LevelEventListener {
    fun onLevelEvent(level: Level, event: LevelEvent)
}