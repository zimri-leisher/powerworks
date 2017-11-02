package screen

import graphics.Image
import graphics.Renderer
import io.*
import main.Game
import misc.GeometryHelper

object IngameGUI : GUIWindow("In game gui",
        0, 0,
        Game.WIDTH, Game.HEIGHT,
        windowGroup = ScreenManager.Groups.BACKGROUND
        ), ControlPressHandler {

    const val MAX_VIEWS = 4
    const val DEFAULT_VIEW_WIDTH = 300
    val DEFAULT_VIEW_HEIGHT = (DEFAULT_VIEW_WIDTH.toDouble() / 16 * 9).toInt()
    const val VIEW_SELECTOR_BUTTON_WIDTH = 5
    const val VIEW_SELECTOR_BUTTON_HEIGHT = 5

    private var viewCount = 0
    private val viewControls = mutableListOf<GUIElement>()
    private var viewControlsOpen = false

    val mainInvGUI = InventoryGUI("In game main inventory gui", "Inventory", Game.mainInv, 20, 20, layer = MAX_VIEWS * 3 + 2)

    init {
        InputManager.registerControlPressHandler(this, Control.TOGGLE_VIEW_CONTROLS)
        adjustDimensions = true
        GUITexturePane(this.rootChild, "In game gui background", 0, 0, Image.GUI.MAIN_MENU_BACKGROUND, Game.WIDTH, Game.HEIGHT).run {
            adjustDimensions = true
            object : GUIElement(this@run, "In game view selector", Game.WIDTH - 30, Game.HEIGHT - 7, 28, 5) {

                val views = arrayOf<GUIWindow>(newView(), newView(), newView(), newView())

                var viewHighlighted = -1

                init {
                    matchParentOpening = false
                    viewControls.add(this)
                }

                private fun newView(): GUIWindow {
                    if(viewCount < MAX_VIEWS) {
                        val newView = ViewWindow("In game view ${viewCount++}",
                                0, 0,
                                DEFAULT_VIEW_WIDTH, DEFAULT_VIEW_HEIGHT,
                                Game.camera,
                                open = viewCount == 1, windowGroup = ScreenManager.Groups.VIEW)
                        newView.run {
                            closeButton.matchParentOpening = false
                            dragGrip.matchParentOpening = false
                            dragGrip.open = viewControlsOpen
                            closeButton.open = viewControlsOpen
                            viewControls.add(dragGrip)
                            viewControls.add(closeButton)
                            viewControls.add(nameText)
                            matchParentOpening = false
                        }
                        return newView
                    }
                    throw Exception("GUI views exceeds limit")
                }

                override fun onMouseActionOn(type: PressType, xPixel: Int, yPixel: Int, button: Int) {
                    if (type == PressType.PRESSED) {
                        // If it is on the space in between one of the four buttons, don't do anything
                        if ((xPixel - this.xPixel) % (VIEW_SELECTOR_BUTTON_WIDTH + 2) > VIEW_SELECTOR_BUTTON_WIDTH) return
                        // Get which view number it is on
                        val viewNum = (xPixel - this.xPixel) / (VIEW_SELECTOR_BUTTON_WIDTH + 2)
                        val view = views[viewNum]
                        view.toggle()
                        if (view.open) {
                            view.children.filter { viewControls.contains(it) }.forEach { it.open = true }
                        }
                    }
                }

                override fun update() {
                    if (GeometryHelper.intersects(Mouse.xPixel, Mouse.yPixel, 0, 0, xPixel, yPixel, widthPixels, heightPixels)) {
                        if ((Mouse.xPixel - this.xPixel) % (VIEW_SELECTOR_BUTTON_WIDTH + 2) > 5) {
                            viewHighlighted = -1; return
                        }
                        val viewNum = (Mouse.xPixel - this.xPixel) / (VIEW_SELECTOR_BUTTON_WIDTH + 2)
                        viewHighlighted = viewNum
                    } else
                        viewHighlighted = -1
                }

                override fun onParentDimensionChange(oldWidth: Int, oldHeight: Int) {
                    relXPixel = Game.WIDTH - 30
                    relYPixel = Game.HEIGHT - 7
                }

                override fun render() {
                    var i = 0
                    for (v in views) {
                        if (i != viewHighlighted)
                            Renderer.renderTexture(if (v.open) Image.GUI.VIEW_SELECTOR_CLOSE_BUTTON else Image.GUI.VIEW_SELECTOR_OPEN_BUTTON, xPixel + i * 7, yPixel)
                        else
                            Renderer.renderTexture(if (v.open) Image.GUI.VIEW_SELECTOR_CLOSE_BUTTON_HIGHLIGHT else Image.GUI.VIEW_SELECTOR_OPEN_BUTTON_HIGHLIGHT, xPixel + i * 7, yPixel)
                        i++
                    }
                }
            }
        }
    }

    override fun handleControlPress(p: ControlPress) {
        if (p.control == Control.TOGGLE_VIEW_CONTROLS && open && p.pressType == PressType.PRESSED) {
            viewControlsOpen = !viewControlsOpen
            viewControls.filter { it.parent.open }.forEach { it.toggle() }
        }
    }

}