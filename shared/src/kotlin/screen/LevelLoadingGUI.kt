package screen

import graphics.Image
import level.RemoteLevel
import main.Game
import main.State
import player.PlayerManager
import screen.elements.GUIText
import screen.elements.GUITexturePane
import screen.elements.GUIWindow

object LevelLoadingGUI : GUIWindow("Level loading window", { 0 }, { 0 }, { Game.WIDTH }, { Game.HEIGHT }, ScreenManager.Groups.BACKGROUND) {

    val text: GUIText

    init {
        GUITexturePane(this, "Level loading screen background", { 0 }, { 0 }, Image.GUI.GREY_FILLER, { widthPixels }, { heightPixels }).run {
            text = GUIText(this, "Loading level levelInfo text", 100, 100, "Loading level")
        }
    }

    override fun update() {
        if(PlayerManager.isLocalPlayerLoaded()) {
            if(PlayerManager.localPlayer.homeLevel is RemoteLevel && (PlayerManager.localPlayer.homeLevel as RemoteLevel).loaded) {
                open = false
                State.setState(State.INGAME)
            }
        }
    }
}