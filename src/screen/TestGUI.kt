package screen

import graphics.Image
import graphics.SyncAnimation
import main.Game

object TestGUI : GUI("Testing GUI", 0, 0, Game.WIDTH, Game.HEIGHT) {

    init {
        GUITexturePane(this, "Test GUI background", 0, 0, Image.GUI.MAIN_MENU_BACKGROUND, Game.WIDTH, Game.HEIGHT).run {
            GUIButton(this, "Test GUI back button", 1, 1, "Back to Main Menu", {
                this@TestGUI.open = false
                MainMenuGUI.open = true
            }, {})
            GUIButton(this, "Test GUI test button", 1, 1 + GUIButton.DEFAULT_HEIGHT, "Add a component", {
                val g = this@TestGUI.get("Test GUI element list")?.children?.add(GUITexturePane(null, "Test GUI texture pane", 0, 0, Image.ERROR))
            }, {})
            GUIElementList(this, "Test GUI element list", 3 + GUIButton.DEFAULT_WIDTH, 1, Game.WIDTH - (3 + GUIButton.DEFAULT_WIDTH), Game.HEIGHT - 1).run {
                GUIButton(this, "Test GUI test button 1", 0, 0, "Test weapon 1 anim", {
                    get("Test texture pane 1")?.toggle()
                    SyncAnimation.WEAPON_1.toggleAndReset()
                }, {}).run {
                    GUITexturePane(this, "Test texture pane 1", GUIButton.DEFAULT_WIDTH + 2, 0, SyncAnimation.WEAPON_1).matchParentOpening = false
                }
                GUIButton(this, "Test GUI test button 2", 0, 0, "Test weapon 2 anim", {
                    get("Test texture pane 2")?.toggle()
                    SyncAnimation.WEAPON_2.toggleAndReset()
                }, {}).run {
                    GUITexturePane(this, "Test texture pane 2", GUIButton.DEFAULT_WIDTH + 2, 0, SyncAnimation.WEAPON_2).matchParentOpening = false
                }
                GUIButton(this, "Test GUI test button 3", 0, 0, "Test weapon 3 anim", {
                    get("Test texture pane 3")?.toggle()
                    //SyncAnimation.WEAPON_3.toggleAndReset()
                }, {}).run {
                    //GUITexturePane(this, "Test texture pane 3", GUIButton.DEFAULT_WIDTH + 2, 0, SyncAnimation.WEAPON_3).matchParentOpening = false
                }
            }
        }
    }

    override fun onClose() {
        //SyncAnimation.WEAPON_3.playing = false
        SyncAnimation.WEAPON_2.playing = false
        SyncAnimation.WEAPON_1.playing = false
        //SyncAnimation.WEAPON_3.reset()
        SyncAnimation.WEAPON_2.reset()
        SyncAnimation.WEAPON_1.reset()
    }
}