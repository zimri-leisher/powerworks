package screen

import graphics.Image
import level.LevelGeneratorSettings
import main.Game
import screen.elements.GUIButton
import screen.elements.GUITexturePane
import screen.elements.GUIWindow

object LevelCreatorGUI : GUIWindow("Level creator", { 0 }, { 0 }, { Game.WIDTH }, { Game.HEIGHT }, windowGroup = ScreenManager.Groups.BACKGROUND) {

    var currentSettings: LevelGeneratorSettings = LevelGeneratorSettings()

    init {
        GUITexturePane(this.rootChild, "Level creator background", { 0 }, { 0 }, Image.GUI.MAIN_MENU_BACKGROUND_FILLER, { widthPixels }, { heightPixels }).run {
            GUIButton(this, "Level create button", { (widthPixels - GUIButton.WIDTH) / 2 }, { heightPixels - GUIButton.HEIGHT - 16 }, "Create level", onRelease = {

            })
        }
    }
}