package player

import level.LevelManager
import network.User
import player.lobby.Lobby
import player.team.Team
import serialization.Input
import serialization.Output
import serialization.Serializer
import java.util.*

class Player(
        val user: User,
        var homeLevelId: UUID,
        var brainRobotId: UUID) {

    private constructor() : this(User(UUID.randomUUID(), ""), UUID.randomUUID(), UUID.randomUUID())

    val homeLevel get() = LevelManager.allLevels.first { it.id == homeLevelId }

    // keep on getting crashes here TODO something about loading before the brain robot is loaded?
    val brainRobot get() = LevelManager.allLevels.flatMap { it.data.brainRobots }.first { it.id == brainRobotId }

    var lobby = Lobby()
    var team = Team(this)

    init {
        PlayerManager.allPlayers.add(this)
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

class PlayerSerializer : Serializer<Player>() {
    override fun write(obj: Any, output: Output) {
        obj as Player
        output.write(obj.user)
        output.write(obj.brainRobotId)
        output.write(obj.homeLevelId)
    }

    override fun instantiate(input: Input): Player {
        val user = input.read(User::class.java)
        val brainRobotId = input.read(UUID::class.java)
        val homeLevelId = input.read(UUID::class.java)
        var alreadyExistingPlayer: Player? = null
        PlayerManager.allPlayers.forEach {
            if (it.user == user) {
                alreadyExistingPlayer = it
            }
        }
        return alreadyExistingPlayer ?: Player(user, homeLevelId, brainRobotId)
    }
}