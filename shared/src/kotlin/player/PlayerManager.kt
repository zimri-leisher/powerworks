package player

import data.FileManager
import data.GameDirectoryIdentifier
import data.WeakMutableList
import item.ItemType
import level.ActualLevel
import level.LevelManager
import level.add
import level.entity.robot.BrainRobot
import network.User
import serialization.SerializerDebugger
import java.util.*

object PlayerManager {
    lateinit var localPlayer: Player

    val allPlayers = WeakMutableList<Player>()

    fun isLocalPlayerLoaded() = ::localPlayer.isInitialized

    fun getPlayer(user: User): Player {

        val alreadyExistingPlayer = allPlayers.filter { it.user == user }.firstOrNull()
        if (alreadyExistingPlayer != null) {
            // player has already been loaded this server session
            return alreadyExistingPlayer
        } else {
            var player = tryLoadPlayer(user.id)
            if (player == null) {
                // player has never connected before
                // will create and save a new level
                player = newPlayer(user)
            } else {
                // player has connected before but not this session
                // preload the players level if it is not already
                if(LevelManager.allLevels.none {it.id == player.homeLevelId}) {
                    ActualLevel(player.homeLevelId, LevelManager.tryLoadLevelInfo(player.homeLevelId)!!)
                }
            }
            return player
        }

    }

    fun tryLoadPlayer(id: UUID) = FileManager.tryLoadObject(GameDirectoryIdentifier.PLAYERS, "$id.player", Player::class.java)

    fun newPlayer(forUser: User): Player {
        val level = ActualLevel(LevelManager.newLevelId(), LevelManager.newLevelInfoFor(forUser))
        val player = Player(forUser, level.id, UUID.randomUUID())
        val brainRobot = BrainRobot(level.widthPixels / 2, level.heightPixels / 2, 0, player)
        for(type in ItemType.ALL) {
            brainRobot.inventory.add(type, type.maxStack)
        }
        level.add(brainRobot)
        player.brainRobotId = brainRobot.id
        LevelManager.saveLevelData(level.id, level.data)
        LevelManager.saveLevelInfo(level.id, level.info)
        return player
    }

    fun savePlayer(player: Player) = FileManager.saveObject(GameDirectoryIdentifier.PLAYERS, "${player.user.id}.player", player)

    fun savePlayers() {
        allPlayers.forEach { savePlayer(it) }
    }
}