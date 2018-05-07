package screen

import graphics.Image
import io.PressType
import level.Level.Companion.indexLevels
import level.LevelGeneratorSettings
import level.LevelInfo
import level.SimplexLevel
import main.Game
import screen.elements.*
import main.State
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import kotlin.streams.toList

object LevelSelectorGUI : GUIWindow("Level selector window", { 0 }, { 0 }, { Game.WIDTH }, { Game.HEIGHT }, windowGroup = ScreenManager.Groups.BACKGROUND) {

    val levelInfos = mutableListOf<LevelInfo>()

    lateinit var infoList: GUIElementList

    class GUILevelInfoDisplay(val levelInfo: LevelInfo, parent: RootGUIElement) : GUIElement(parent, "level info for level ${levelInfo.name}", 0, 0, WIDTH, HEIGHT) {

        init {
            GUIDefaultTextureRectangle(this, name + " background", 0, 0).run {
                transparentToInteraction = true
                GUIText(this, this@GUILevelInfoDisplay.name + " level name text", 6, 4, levelInfo.name).run {
                    transparentToInteraction = true
                }
            }
        }

        override fun onMouseActionOn(type: PressType, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
            Game.currentLevel = SimplexLevel(levelInfo)
            LevelSelectorGUI.open = false
            State.setState(State.INGAME)
        }

        companion object {
            val WIDTH = GUIButton.WIDTH
            val HEIGHT = GUIButton.HEIGHT + 8
        }
    }

    init {
        adjustDimensions = true
        indexLevels()
        GUITexturePane(this.rootChild, "background texture", { 0 }, { 0 }, Image.GUI.MAIN_MENU_BACKGROUND_FILLER, { widthPixels }, { heightPixels }).run {
            val group = AutoFormatGUIGroup(this, "level button auto format group", 4, 4, accountForChildHeight = true, yPixelSeparation = 2, initializerList = {
                GUIButton(this, "main menu return button", 0, 0, "Return to main menu", onRelease = {
                    this@LevelSelectorGUI.open = false
                    MainMenuGUI.open = true
                })
                GUIButton(this, "level create button", 0, 0, "Create save", onRelease = {
                    val info = LevelInfo("testinglevel", LocalDateTime.now().toString(), LevelGeneratorSettings(256, 256), File(""), File(""))
                    indexLevels()
                    GUILevelInfoDisplay(info, infoList.elements)
                })
            })
            GUIDefaultTextureRectangle(this, "level info list background", { 8 + group.widthPixels }, { 4 }, { GUILevelInfoDisplay.WIDTH + GUIVerticalScrollBar.WIDTH + 4 }, { heightPixels - 8 }).run {
                infoList = GUIElementList(this, "level info list", { 2 }, { 2 }, { widthPixels - 4 }, { heightPixels - 4 }, {
                    for (info in levelInfos) {
                        GUILevelInfoDisplay(info, this)
                    }
                })
            }
        }
    }
}