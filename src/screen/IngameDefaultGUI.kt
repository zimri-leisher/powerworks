package screen

import main.Game

object IngameDefaultGUI : GUI("In game default gui", 0, 0, Game.WIDTH, Game.HEIGHT) {
    init {
        GUIView(this, "In game default view", 0, 0, Game.WIDTH, Game.HEIGHT, camera = Game.player)
    }
}