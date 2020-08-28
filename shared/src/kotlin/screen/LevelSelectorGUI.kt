package screen

import graphics.Image
import level.LevelManager
import main.Game
import main.heightPixels
import main.widthPixels
import network.ClientNetworkManager
import network.packet.RequestLevelDataPacket
import network.packet.RequestLoadGamePacket
import network.packet.RequestPlayerDataPacket
import screen.elements.GUIButton
import screen.elements.GUITexturePane
import screen.elements.GUIWindow

object LevelSelectorGUI : GUIWindow("Level selector window", { 0 }, { 0 }, { Game.WIDTH }, { Game.HEIGHT }, layer = 3) {


    init {
        GUITexturePane(this, "background renderable", { 0 }, { 0 }, Image.GUI.GREY_FILLER, { widthPixels }, { heightPixels }).apply {
            GUIButton(this, "main menu return button", { 2 }, { 2 }, "<size=40>B\nA\nC\nK", true, { (19 * (Game.WIDTH.toFloat() / 300)).toInt() }, { Game.HEIGHT - 4 }, onRelease = {
                this@LevelSelectorGUI.open = false
                MainMenuGUI.open = true
            })

            GUIButton(this, "level play button", { Game.WIDTH - (19 * (Game.WIDTH.toFloat() / 300)).toInt() - 2 }, { 2 }, "<size=40>P\nL\nA\nY", true, { (19 * (Game.WIDTH.toFloat() / 300)).toInt() }, { Game.HEIGHT - 4 },
                    onRelease = {
                        this@LevelSelectorGUI.open = false
                        ClientNetworkManager.sendToServer(RequestLoadGamePacket(Game.USER))
                        LevelLoadingGUI.open = true
                    }).apply {
                if(!ClientNetworkManager.hasConnected()) {
                    available = false
                    notAvailableMessage = "Unable to connect to server!"
                }
                // keep their ratios the same
                GUITexturePane(this, "level selector play button warning stripes 1", { 1 }, { 1 }, Image.GUI.WARNING_STRIPES,
                        { this.widthPixels - 2 }, { ((this.widthPixels - 2) * (Image.GUI.WARNING_STRIPES.heightPixels.toFloat() / Image.GUI.WARNING_STRIPES.widthPixels.toFloat())).toInt() }).transparentToInteraction = true

                GUITexturePane(this, "level selector play button warning stripes 2", { 1 }, { heightPixels - 1 - ((this.widthPixels - 2) * (Image.GUI.WARNING_STRIPES.heightPixels.toFloat() / Image.GUI.WARNING_STRIPES.widthPixels.toFloat())).toInt() }, Image.GUI.WARNING_STRIPES,
                        { this.widthPixels - 2 }, { ((this.widthPixels - 2) * (Image.GUI.WARNING_STRIPES.heightPixels.toFloat() / Image.GUI.WARNING_STRIPES.widthPixels.toFloat())).toInt() }).transparentToInteraction = true
            }
        }
    }
}