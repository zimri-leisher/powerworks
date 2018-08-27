package screen

import graphics.Image
import graphics.text.TextRenderParams
import main.Game
import screen.elements.*

internal object TestGUI : GUIWindow("Testing GUI", 0, 0, Game.WIDTH, Game.HEIGHT, windowGroup = ScreenManager.Groups.BACKGROUND) {

    lateinit var text: GUIText

    init {
        GUITexturePane(this, "Test GUI background", { 0 }, { 0 }, Image.GUI.MAIN_MENU_BACKGROUND, { Math.max(Game.WIDTH, Image.GUI.MAIN_MENU_BACKGROUND.widthPixels) }, { Math.max(Game.HEIGHT, Image.GUI.MAIN_MENU_BACKGROUND.heightPixels) }).run {
            GUIButton(this, "Test GUI back button", 1, 1, "Back to Main Menu", onRelease = {
                this@TestGUI.open = false
                MainMenuGUI.open = true
            })
            text = GUIText(this, "text", 40, 80, "|italics|t", allowTags = true).apply {
                GUIOutline(this, "fasdfasdf")
            }
            GUITextInputField(this, "test", { 40 }, { 40 }, 50, 2, onPressEnter = { currentText ->
                this@TestGUI.text.text = currentText
            }).apply {
                autocompleteMenu.options.addAll(listOf("<color=", "<style=", "<italic>", "<corlasdf>"))
            }
        }
    }
}