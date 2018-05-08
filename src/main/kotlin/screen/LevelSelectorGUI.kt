package screen

import graphics.Font
import graphics.Image
import level.Level
import level.LevelGeneratorSettings
import level.LevelInfo
import level.SimplexLevel
import main.Game
import screen.elements.*
import main.State
import java.io.File
import java.time.LocalDateTime

object LevelSelectorGUI : GUIWindow("Level selector window", { 0 }, { 0 }, { Game.WIDTH }, { Game.HEIGHT }, windowGroup = ScreenManager.Groups.BACKGROUND) {

    lateinit var infoList: GUIElementList

    class GUILevelInfoDisplay(val levelInfo: LevelInfo, parent: RootGUIElement) : GUIElement(parent, "level info for level ${levelInfo.name}", 0, 0, WIDTH, HEIGHT, parent.open) {

        init {
            GUIButton(this, name + " button", 0, 0, levelInfo.name, widthPixels, heightPixels, onRelease = {
                Game.currentLevel = SimplexLevel(levelInfo)
                LevelSelectorGUI.open = false
                State.setState(State.INGAME)
            }, open = open)
        }

        companion object {
            val WIDTH = GUIButton.WIDTH
            val HEIGHT = GUIButton.HEIGHT + 8
        }
    }

    init {
        adjustDimensions = true
        Level.indexLevels()
        GUITexturePane(this.rootChild, "background texture", { 0 }, { 0 }, Image.GUI.MAIN_MENU_BACKGROUND_FILLER, { widthPixels }, { heightPixels }).run {
            val group = AutoFormatGUIGroup(this, "level menu buttons auto format group", 4, 4, accountForChildHeight = true, yPixelSeparation = 2, initializerList = {
                GUIButton(this, "main menu return button", 0, 0, "Return to main menu", onRelease = {
                    this@LevelSelectorGUI.open = false
                    MainMenuGUI.open = true
                })
            })
            GUIText(this, "Level selector choice prompt text", { (this@LevelSelectorGUI.widthPixels - Font.getStringBounds("Select level").width) / 2 }, {4}, "Select level")
            val e = GUIDefaultTextureRectangle(this, "level info list background", { (this@LevelSelectorGUI.widthPixels - GUILevelInfoDisplay.WIDTH - GUIVerticalScrollBar.WIDTH) / 2 }, { 12 }, { GUILevelInfoDisplay.WIDTH + GUIVerticalScrollBar.WIDTH + 4 }, { heightPixels - 16 }).apply {
                infoList = GUIElementList(this, "level info list", { 2 }, { 2 }, { widthPixels - 4 }, { heightPixels - 4 }, {
                    for (info in Level.levelInfos) {
                        GUILevelInfoDisplay(info, this)
                    }
                })
            }
            AutoFormatGUIGroup(this, "level modification buttons auto format group", {e.xAlignment() + e.widthAlignment() + 4}, {e.yAlignment()}, initializerList = {
                GUIButton(this, "level create button", 0, 0, "Create level", onRelease = {
                    var i = 0
                    while (Level.exists("testinglevel$i"))
                        i++
                    val info = LevelInfo("testinglevel$i", LocalDateTime.now().toString(), LevelGeneratorSettings(256, 256), File(""), File(""))
                    Level.levelInfos.add(info)
                    GUILevelInfoDisplay(info, infoList)
                })
            }, accountForChildHeight = true, yPixelSeparation = 2)
        }
    }
}