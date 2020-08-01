package player.team

import player.Player
import serialization.Id

class Team(
        @Id(1)
        val players: List<Player>) {

    private constructor() : this(listOf())

    constructor(player: Player) : this(listOf(player))

    fun check(permission: TeamPermission, player: Player): Boolean {
        if(players.isEmpty()) {
            return true
        }
        return player in players
    }

    companion object {
        val NEUTRAL = Team(listOf())
    }
}