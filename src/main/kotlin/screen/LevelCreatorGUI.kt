package screen

import main.Game
import screen.elements.GUITexturePane
import screen.elements.GUIWindow

object LevelCreatorGUI : GUIWindow("Level creator", { 0 }, { 0 }, { Game.WIDTH }, { Game.HEIGHT }, windowGroup = ScreenManager.Groups.BACKGROUND) {
    init {
    }
}