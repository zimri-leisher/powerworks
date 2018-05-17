package screen

import graphics.Image
import level.Level
import level.LevelGeneratorSettings
import level.LevelInfo
import main.Game
import screen.elements.*
import java.io.File
import java.time.LocalDateTime

object LevelCreatorGUI : GUIWindow("Level creator", { 0 }, { 0 }, { Game.WIDTH }, { Game.HEIGHT }, windowGroup = ScreenManager.Groups.BACKGROUND) {

    var currentName = ""
    var currentSettings = LevelGeneratorSettings()

    init {
        GUITexturePane(this.rootChild, "Level creator background", { 0 }, { 0 }, Image.GUI.MAIN_MENU_BACKGROUND_FILLER, { widthPixels }, { heightPixels }).run {
            GUIButton(this, "Level create button", { (widthPixels - GUIButton.WIDTH) / 2 }, { heightPixels - GUIButton.HEIGHT - 16 }, "Create level", onRelease = {
                Level.levelInfos.add(LevelInfo(currentName, LocalDateTime.now().toString(), currentSettings, File(""), File("")))
            })
            AutoFormatGUIGroup(this, "Level creator options group 1", { (widthPixels - GUIButton.WIDTH * 2) / 2 }, { 16 }, initializerList = {
                GUITextInputField(this, "Level creator name input", { 0 }, { 0 }, { GUIButton.WIDTH }, { 8 }, defaultValue = "Level", onEnterChar = { text ->
                    currentName = text
                })
            }, accountForChildHeight = true, yPixelSeparation = 1)
        }
    }
}