package screen

import graphics.Image
import level.Level
import level.LevelGeneratorSettings
import level.LevelInfo
import level.LevelManager
import main.Game
import screen.elements.*
import java.io.File
import java.time.LocalDateTime

object LevelCreatorGUI : GUIWindow("Level creator", { 0 }, { 0 }, { Game.WIDTH }, { Game.HEIGHT }, ScreenManager.Groups.BACKGROUND) {

    var currentName = ""
    var currentSettings = LevelGeneratorSettings()

    init {
        GUITexturePane(this, "Level creator background", { 0 }, { 0 }, Image.GUI.GREY_FILLER, { widthPixels }, { heightPixels }).run {
            GUIButton(this, "Level create button", { (widthPixels - GUIButton.WIDTH) / 2 }, { heightPixels - GUIButton.HEIGHT - 16 }, "Create level", onRelease = {
                LevelManager.levelInfos.add(LevelInfo(currentName, LocalDateTime.now().toString(), currentSettings, File(""), File("")))
            })
            AutoFormatGUIGroup(this, "Level creator options group 1", { (widthPixels - GUIButton.WIDTH * 2) / 2 }, { 16 }, initializerList = {
                GUITextInputField(this, "Level creator name input", { 0 }, { 0 }, 32, 1, defaultValue = "Level", onEnterText = { text, _ ->
                    currentName = text
                })
            }, accountForChildHeight = true, yPixelSeparation = 1)
        }
    }
}