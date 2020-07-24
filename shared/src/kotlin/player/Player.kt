package player

import level.LevelManager
import network.User
import player.lobby.Lobby
import serialization.Id
import java.util.*

class Player(
        @Id(1)
        val user: User,
        @Id(2)
        var homeLevelId: UUID,
        @Id(3)
        var brainRobotId: UUID) {

    private constructor() : this(User(UUID.randomUUID(), ""), UUID.randomUUID(), UUID.randomUUID())

    val homeLevel get() = LevelManager.allLevels.first { it.id == homeLevelId }

    // keep on getting crashes here TODO something about loading before the brain robot is loaded?
    val brainRobot get() = LevelManager.allLevels.flatMap { it.data.brainRobots }.first { it.id == brainRobotId }

    var lobby = Lobby()

    init {
        var alreadyExists = false
        PlayerManager.allPlayers.forEach {
            if(it.user == user) {
                alreadyExists = true
            }
        }
        if(!alreadyExists) {
            PlayerManager.allPlayers.add(this)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Player

        if (user != other.user) return false
        if (homeLevelId != other.homeLevelId) return false
        if (brainRobotId != other.brainRobotId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = user.hashCode()
        result = 31 * result + homeLevelId.hashCode()
        result = 31 * result + brainRobotId.hashCode()
        return result
    }
}