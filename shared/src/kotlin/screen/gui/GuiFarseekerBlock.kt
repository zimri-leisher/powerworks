package screen.gui

import level.LevelManager
import level.RemoteLevel
import level.block.FarseekerBlock
import network.BlockReference
import player.ActionFarseekerBlockSetLevel
import player.PlayerManager
import screen.ScreenLayer
import screen.element.ElementLevelSelector

class GuiFarseekerBlock(block: FarseekerBlock) : Gui(ScreenLayer.MENU_1), PoolableGui {

    var block = block
        set(value) {
            if (field != value) {
                field = value
                levelSelector.levels = value.availableDestinations
                if (value.destinationLevel != null) {
                    levelSelector.selectedLevel = value.destinationLevel!!.id to value.destinationLevel!!.info
                }
            }
        }

    lateinit var levelSelector: ElementLevelSelector

    init {
        define {
            openAtCenter(-1)
            keepInsideScreen()
            background {
                makeDraggable()
                dimensions = Dimensions.FitChildren.pad(4, 9)
                closeButton(Placement.Align(HorizontalAlign.RIGHT, VerticalAlign.TOP).offset(-1, -1))
                text("Farseeker", Placement.Align(HorizontalAlign.LEFT, VerticalAlign.TOP).offset(1, -1))
                levelSelector = levelSelector(block.availableDestinations, { uuid, levelInfo ->
                    PlayerManager.takeAction(ActionFarseekerBlockSetLevel(PlayerManager.localPlayer, block, LevelManager.getLevelByIdOrNull(uuid)
                            ?: RemoteLevel(uuid, levelInfo).apply { initialize() }))
                }, Placement.Align(HorizontalAlign.CENTER, VerticalAlign.BOTTOM).offset(0, 2))
                if (block.destinationLevel != null) {
                    levelSelector.selectedLevel = block.destinationLevel!!.id to block.destinationLevel!!.info
                }
            }
        }
    }

    override fun canDisplay(obj: Any?) = obj is FarseekerBlock

    override fun display(obj: Any?) {
        block = obj as FarseekerBlock
    }

    override fun isDisplaying(obj: Any?) = obj == block

}