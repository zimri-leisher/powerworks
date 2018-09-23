package screen

import graphics.Image
import graphics.text.TextManager
import io.PressType
import level.Level
import level.LevelGeneratorSettings
import level.LevelInfo
import level.SimplexLevel
import main.Game
import main.State
import misc.Numbers
import screen.elements.*
import java.io.File
import java.time.LocalDateTime

object LevelSelectorGUI : GUIWindow("Level selector window", { 0 }, { 0 }, { Game.WIDTH }, { Game.HEIGHT }, ScreenManager.Groups.BACKGROUND, layer = 1) {

    lateinit var infoList: GUIElementList

    class GUILevelInfoDisplay(val levelInfo: LevelInfo, parent: RootGUIElement) : GUIElement(parent, "level info for level ${levelInfo.name}", 0, 0, WIDTH, HEIGHT, parent.open) {

        init {
            GUIDefaultTextureRectangle(this, name + " button background", 0, 0, open = open).apply {
                val rect = this
                GUIText(this, name + " info text", 0, 0, levelInfo.name, open = open).apply {
                    transparentToInteraction = true
                    alignments.x = { (rect.widthPixels - widthPixels) / 2 }
                    alignments.y = { (rect.heightPixels - Numbers.ceil(TextManager.getFont().charHeight)) / 2 }
                }
                GUIClickableRegion(this, this@GUILevelInfoDisplay.name + " click region", { 0 }, { 0 }, { widthPixels }, { heightPixels }, { type, xPixel, yPixel, button, shift, ctrl, alt ->
                    if (type == PressType.RELEASED) {
                        Game.currentLevel = SimplexLevel(levelInfo)
                        LevelSelectorGUI.open = false
                        State.setState(State.INGAME)
                    }
                }, open = open)
            }
        }

        companion object {
            const val WIDTH = GUIButton.WIDTH
            const val HEIGHT = GUIButton.HEIGHT + 8
        }
    }

    init {
        GUITexturePane(this, "background texture", { 0 }, { 0 }, Image.GUI.GREY_FILLER, { widthPixels }, { heightPixels }).run {
            AutoFormatGUIGroup(this, "level menu buttons auto format group", 4, heightPixels - 4, accountForChildHeight = true, yPixelSeparation = 2, flipY = true, initializerList = {
                val parsed = TextManager.parseTags("<size=40><img=misc/back_arrow>")
                GUIButton(this, "main menu return button", 0, 0, "<size=40><img=misc/back_arrow>", true, TextManager.getStringBounds(parsed).width + 4, TextManager.getStringBounds(parsed).height + 4, onRelease = {
                    this@LevelSelectorGUI.open = false
                    MainMenuGUI.open = true
                })
            })
            GUIText(this, "Level selector choice prompt text", { (this@LevelSelectorGUI.widthPixels - TextManager.getStringBounds("Select level").width) / 2 }, { heightPixels - 4 }, "Select level")
            val infoList = GUIDefaultTextureRectangle(this, "level info list background", { (this@LevelSelectorGUI.widthPixels - GUILevelInfoDisplay.WIDTH - GUIVerticalScrollBar.WIDTH) / 2 }, { 12 }, { GUILevelInfoDisplay.WIDTH + GUIVerticalScrollBar.WIDTH + 4 }, { heightPixels - 16 }).apply {
                infoList = GUIElementList(this, "level info list", { 2 }, { 2 }, { widthPixels - 4 }, { heightPixels - 2 }, {
                    for (info in Level.levelInfos) {
                        GUILevelInfoDisplay(info, this)
                    }
                })
            }
            AutoFormatGUIGroup(this, "level modification buttons auto format group", { infoList.alignments.x() + infoList.alignments.width() + 4 }, { infoList.alignments.y() + infoList.alignments.height() }, initializerList = {
                GUIButton(this, "level create button", 0, 0, "Create level", onRelease = {
                    var i = 0
                    while (Level.exists("testinglevel$i"))
                        i++
                    val info = LevelInfo("testinglevel$i", LocalDateTime.now().toString(), LevelGeneratorSettings(256, 256), File(""), File(""))
                    Level.levelInfos.add(info)
                    GUILevelInfoDisplay(info, this@LevelSelectorGUI.infoList)
                })
            }, accountForChildHeight = true, yPixelSeparation = 2, flipY = true)
        }
    }
}