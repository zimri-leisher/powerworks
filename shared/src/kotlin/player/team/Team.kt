package player.team

import player.Player
import serialization.Id

class Team(
        @Id(1)
        val players: List<Player>,
        val color: Int = 0x0000FF) {

    private constructor() : this(listOf())

    constructor(player: Player) : this(listOf(player))

    fun check(permission: TeamPermission, player: Player): Boolean {
        if (players.isEmpty()) {
            return true
        }
        return player in players
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Team

        if (players != other.players) return false

        return true
    }

    override fun hashCode(): Int {
        return players.hashCode()
    }

    companion object {
        val NEUTRAL = Team(listOf())
    }
}