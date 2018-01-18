package screen

import crafting.Crafter
import screen.elements.GUIWindow

class CraftingGUI(name: String,
                  xAlignment: () -> Int, yAlignment: () -> Int,
                  val crafters: List<Crafter>) :
        GUIWindow("", xAlignment, yAlignment, { 0 }, { 0 }, windowGroup = ScreenManager.Groups.PLAYER_UTIL) {

}