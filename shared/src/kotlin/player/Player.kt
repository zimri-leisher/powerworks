package player

import level.Level
import level.LevelEvent
import level.LevelEventListener
import level.LevelManager
import level.entity.robot.BrainRobot
import main.Game
import main.PowerworksDelegates
import network.ClientNetworkManager
import network.ServerNetworkManager
import network.User
import player.lobby.Lobby
import player.team.Team
import resource.ResourceTransactionExecutor
import serialization.*
import java.util.*

class Player(
    val user: User,
    var homeLevelId: UUID,
    var brainRobotId: UUID
) : LevelEventListener {

    var online = false
        set(value) {
            if (field != value) {
                if (!value) {
                    playing = false
                }
                field = value
                println("online: $field")
                PlayerManager.pushPlayerEvent(this, if (field) PlayerEvent.CONNECT else PlayerEvent.DISCONNECT)
            }
        }

    var playing = false
        set(value) {
            if (field != value) {
                if (value) {
                    online = true
                }
                field = value
                println("playing: $field")
                PlayerManager.pushPlayerEvent(this, if (field) PlayerEvent.START_PLAYING else PlayerEvent.STOP_PLAYING)
            }
        }

    private val homeLevelDelagate = PowerworksDelegates.lateinitVal<Level>()
    var homeLevel: Level by homeLevelDelagate
    val isHomeLevelInitialized get() = LevelManager.isLevelInitialized(homeLevelId)
    val isHomeLevelLoaded get() = LevelManager.isLevelLoaded(homeLevelId)

    private val brainRobotDelegate = PowerworksDelegates.lateinitVal<BrainRobot>()
    var brainRobot: BrainRobot by brainRobotDelegate

    val resourceTransactionExecutor = ResourceTransactionExecutor.Player(this)

    var lobby = Lobby()
    var team = Team(this)

    fun initialize() {
        LevelManager.levelEventListeners.add(this)
        PlayerManager.pushPlayerEvent(this, PlayerEvent.INITIALIZE)
        online =
            if (Game.IS_SERVER) ServerNetworkManager.isOnline(user) else ClientNetworkManager.isVisibleAndOnline(user)
        playing = LevelManager.isLevelLoaded(homeLevelId)
    }

    override fun onLevelEvent(level: Level, event: LevelEvent) {
        if (event == LevelEvent.INITIALIZE) {
            if (level.id == homeLevelId && !homeLevelDelagate.initialized) {
                homeLevel = level
            }
        } else if (event == LevelEvent.LOAD) {
            if (!brainRobotDelegate.initialized) {
                for (brainRobot in level.data.brainRobots) {
                    if (brainRobot.id == brainRobotId) {
                        this.brainRobot = brainRobot
                    }
                }
            }
            if (level.id == homeLevelId) {
                playing = true
            }
        } else if (event == LevelEvent.UNLOAD) {
            if (level.id == homeLevelId) {
                playing = false
            }
        }
    }

    override fun toString(): String {
        return "Player(user=$user, homeLevelId=$homeLevelId, brainRobotId=$brainRobotId)"
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

class PlayerSerializer(type: Class<Player>, settings: List<SerializerSetting<*>>) : Serializer<Player>(type, settings) {

    override val writeStrategy = object : WriteStrategy<Player>(type) {
        override fun write(obj: Player, output: Output) {
            output.write(obj.user)
            output.write(obj.brainRobotId)
            output.write(obj.homeLevelId)
        }
    }

    override val createStrategy = object : CreateStrategy<Player>(type) {
        override fun create(input: Input): Player {
            val user = input.read(User::class.java)
            val brainRobotId = input.read(UUID::class.java)
            val homeLevelId = input.read(UUID::class.java)
            val alreadyExistingPlayer = PlayerManager.getInitializedPlayerOrNull(user)
            if (alreadyExistingPlayer != null) {
                return alreadyExistingPlayer
            }
            val newPlayer = Player(user, homeLevelId, brainRobotId)
            newPlayer.initialize()
            return newPlayer
        }
    }
}