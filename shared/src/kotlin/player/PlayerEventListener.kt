package player

import level.Level

enum class PlayerEvent {
    CONNECT, DISCONNECT, START_PLAYING, STOP_PLAYING, INITIALIZE
}

interface PlayerEventListener {
    fun onPlayerEvent(player: Player, event: PlayerEvent)
}