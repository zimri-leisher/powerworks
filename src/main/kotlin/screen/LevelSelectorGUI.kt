package screen

import graphics.Image
import graphics.text.TextManager
import io.PressType
import level.*
import main.Game
import main.State
import main.heightPixels
import main.widthPixels
import misc.Numbers
import screen.animations.SlideOffScreenAnimation
import screen.elements.*
import java.io.File
import java.time.LocalDateTime

object LevelSelectorGUI : GUIWindow("Level selector window", { 0 }, { 0 }, { Game.WIDTH }, { Game.HEIGHT }, ScreenManager.Groups.BACKGROUND, layer = 3) {

    var selectionList: GUIElementList

    var playButton: GUIButton

    val selectionHighlight = GUITexturePane(this, "level selector button highlight",
            0, 0, Image.GUI.LEVEL_SELECTOR_BUTTON_HIGHLIGHT).apply { matchParentOpening = false }

    var selectedLevelSelector: GUILevelSelectionButton? = null
        set(value) {
            if (field != value) {
                if (value != null) {
                    selectionHighlight.open = true
                    selectionHighlight.parent = value
                    selectedLevelInfo = value.levelInfo
                    selectionHighlight.layer = value.background.layer + 1
                    if (field != null) {
                        field!!.background.localRenderParams.apply {
                            brightness = 1f
                            rotation = 0f
                        }
                    }
                } else {
                    selectedLevelInfo = null
                }
                field = value
            }
        }

    var selectedLevelInfo: LevelInfo? = null
        set(value) {
            playButton.available = value != null
            field = value
        }

    class GUILevelSelectionButton(val levelInfo: LevelInfo, parent: RootGUIElement) : GUIElement(parent, "level selection button for level ${levelInfo.name}", 0, 0, WIDTH, HEIGHT, parent.open) {

        val background: GUIDefaultTextureRectangle

        override fun onMouseEnter() {
            if (this != selectedLevelSelector) {
                background.localRenderParams.brightness = 1.2f
            }
        }

        override fun onMouseLeave() {
            if (this != selectedLevelSelector) {
                background.localRenderParams.brightness = 1f
            }
        }

        init {
            background = GUIDefaultTextureRectangle(this, name + " button background", 0, 0, open = open).apply {
                val rect = this
                GUIText(this, name + " info text", 0, 0, levelInfo.name, open = open).apply {
                    transparentToInteraction = true
                    alignments.x = { (rect.widthPixels - widthPixels) / 2 }
                    alignments.y = { (rect.heightPixels - Numbers.ceil(TextManager.getFont().charHeight)) / 2 }
                }

                GUIClickableRegion(this, this@GUILevelSelectionButton.name + " click region", { 0 }, { 0 }, { widthPixels }, { heightPixels },
                        { type, xPixel, yPixel, button, shift, ctrl, alt ->
                            if (type == PressType.PRESSED) {
                                selectedLevelSelector = this@GUILevelSelectionButton
                                rect.localRenderParams.rotation = 180f
                                rect.localRenderParams.brightness = 0.9f
                            }
                        }, open = open).layer
            }
        }

        companion object {
            const val WIDTH = GUIButton.WIDTH
            const val HEIGHT = GUIButton.HEIGHT + 8
        }
    }

    class GUILevelInfoDisplay(parent: RootGUIElement, xAlignment: Alignment, yAlignment: Alignment, widthAlignment: Alignment, heightAlignment: Alignment,
                              val levelInfo: LevelInfo) : GUIElement(parent, "level info display for level ${levelInfo.name}", xAlignment, yAlignment, widthAlignment, heightAlignment) {
        init {
            GUIDefaultTextureRectangle(this, "gui level info display background", 0, 0).localRenderParams.brightness = 0.8f
        }
    }

    init {
        GUITexturePane(this, "background renderable", { 0 }, { 0 }, Image.GUI.GREY_FILLER, { widthPixels }, { heightPixels }).apply {
            val returnButton = GUIButton(this, "main menu return button", { 2 }, { 2 }, "<size=40>B\nA\nC\nK", true, { (19 * (Game.WIDTH.toFloat() / 300)).toInt() }, { Game.HEIGHT - 4 }, onRelease = {
                this@LevelSelectorGUI.open = false
                MainMenuGUI.open = true
            })

            val selectionList = GUIDefaultTextureRectangle(this, "level selection list background", { returnButton.alignments.x() + returnButton.widthPixels + 4 }, { 9 }, { GUILevelSelectionButton.WIDTH + GUIVerticalScrollBar.WIDTH + 4 }, { heightPixels - 16 }).apply {

                selectionList = GUIElementList(this, "level selection list", { 2 }, { 2 }, { widthPixels - 4 }, { heightPixels - 4 }, {
                    for (info in LevelManager.levelInfos) {
                        GUILevelSelectionButton(info, this)
                    }
                })

                GUIText(this, "Level selector choice prompt text", { (widthPixels - TextManager.getStringWidth("Select level")) / 2 }, { heightPixels + 1 }, "Select level")
            }

            playButton = GUIButton(this, "level play button", { Game.WIDTH - (19 * (Game.WIDTH.toFloat() / 300)).toInt() - 2 }, { 2 }, "<size=40>P\nL\nA\nY", true, { (19 * (Game.WIDTH.toFloat() / 300)).toInt() }, { Game.HEIGHT - 4 },
                    available = false, notAvailableMessage = "Select a level first!",
                    onRelease = {
                        LevelManager.setLocalLevel(SimplexLevel(selectedLevelInfo!!))
                        State.setState(State.INGAME)
                        SlideOffScreenAnimation(this@LevelSelectorGUI, onStop = {
                            this@LevelSelectorGUI.open = false
                            IngameGUI.open = true
                        }).playing = true
                    }).apply {

                // keep their ratios the same
                GUITexturePane(this, "level selector play button warning stripes 1", { 1 }, { 1 }, Image.GUI.WARNING_STRIPES,
                        { this.widthPixels - 2 }, { ((this.widthPixels - 2) * (Image.GUI.WARNING_STRIPES.heightPixels.toFloat() / Image.GUI.WARNING_STRIPES.widthPixels.toFloat())).toInt() }).transparentToInteraction = true

                GUITexturePane(this, "level selector play button warning stripes 2", { 1 }, { heightPixels - 1 - ((this.widthPixels - 2) * (Image.GUI.WARNING_STRIPES.heightPixels.toFloat() / Image.GUI.WARNING_STRIPES.widthPixels.toFloat())).toInt() }, Image.GUI.WARNING_STRIPES,
                        { this.widthPixels - 2 }, { ((this.widthPixels - 2) * (Image.GUI.WARNING_STRIPES.heightPixels.toFloat() / Image.GUI.WARNING_STRIPES.widthPixels.toFloat())).toInt() }).transparentToInteraction = true
            }

            AutoFormatGUIGroup(this, "level modification buttons auto format group", { selectionList.alignments.x() + selectionList.alignments.width() + 4 }, { selectionList.alignments.y() + selectionList.alignments.height() }, initializerList = {

                GUIButton(this, "level create button", 0, 0, "Create level", onRelease = {
                    var i = 0
                    while (LevelManager.exists("testinglevel$i"))
                        i++
                    val info = LevelInfo("testinglevel$i", LocalDateTime.now().toString(), LevelGeneratorSettings(256, 256), File(""), File(""))
                    LevelManager.levelInfos.add(info)
                    this@LevelSelectorGUI.selectionList.add(GUILevelSelectionButton(info, this@LevelSelectorGUI.selectionList))
                })

            }, accountForChildHeight = true, yPixelSeparation = 2, flipY = true)
        }
    }

    override fun onOpen() {
        if(selectedLevelInfo != null) {
            selectionHighlight.open = true
        }
    }
}