package player.team

import player.Player
import serialization.Id

class Team(
        @Id(1)
        val players: Set<Player>,
        val color: Int = 0x0000FF) {

    private constructor() : this(setOf())

    constructor(player: Player) : this(setOf(player))

    fun check(permission: TeamPermission, player: Player): Boolean {
        if (players.isEmpty()) {
            return true
        }
        return player in players
    }

    override fun toString(): String {
        return players.toString()
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
        val NEUTRAL = Team(setOf())
    }
}