package player.team

class Team(val name: String) {
    private val allies = mutableListOf<Team>()
    private val enemies = mutableListOf<Team>()
    fun setAllyWith(t: Team) {
        allies.add(t)
        enemies.remove(t)
    }

    fun setEnemyWith(t: Team) {
        enemies.add(t)
        allies.remove(t)
    }

    fun setNeutralWith(t: Team) {
        enemies.remove(t)
        allies.remove(t)
    }

    fun isAlly(t: Team) = t in allies

    fun isEnemy(t: Team) = t in enemies

    fun isNeutral(t: Team) = !isAlly(t) && !isEnemy(t)
}