package progression

import level.LevelInfo
import level.LevelManager
import network.ServerNetworkManager
import network.packet.*
import player.Player
import player.PlayerManager
import java.util.*

object ProgressionManager {

    fun getAvailableEnemyLevels(player: Player): Map<UUID, LevelInfo> {
        val enemies = PlayerManager.allPlayers.elements.filter { it != player && it.online }
        return enemies.mapNotNull { enemy -> LevelManager.allLevels.firstOrNull { it.id == enemy.homeLevelId } }.map { (it.id to it.info) }.toMap()
    }

}