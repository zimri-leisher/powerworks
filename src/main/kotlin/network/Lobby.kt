package network

import level.Level
import player.Player

class Lobby {
    val players = mutableListOf<Player>()
    val levels = mutableListOf<Level>()
}