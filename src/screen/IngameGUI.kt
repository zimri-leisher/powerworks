package screen

import graphics.Image
import graphics.Renderer
import io.*
import level.LevelObject
import main.Game
import misc.GeometryHelper
import player.Camera
import screen.IngameGUI.views

private var viewCount = 0
private val viewControls = mutableListOf<RootGUIElement>()
private var viewControlsOpen = false

object ViewControlGUI : GUIWindow("In game view selector window", { Game.WIDTH - 30 }, { Game.HEIGHT - 7 }, { 28 }, { 5 }, windowGroup = ScreenManager.Groups.HOTBAR) {

    val VIEW_SELECTOR_BUTTON_WIDTH = 5
    var viewHighlighted = -1

    init {
        children.add(
                object : GUIElement(this, "In game view selector", 0, 0, 28, 5) {
                    init {
                        viewControls.add(this)
                    }

                    override fun onMouseActionOn(type: PressType, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
                        if (type == PressType.PRESSED) {
                            // If it is on the space in between one of the four buttons, don't do anything
                            if ((xPixel - this.xPixel) % (VIEW_SELECTOR_BUTTON_WIDTH + 2) > VIEW_SELECTOR_BUTTON_WIDTH) return
                            // Get which view number it is on
                            val viewNum = (xPixel - this.xPixel) / (VIEW_SELECTOR_BUTTON_WIDTH + 2)
                            val view = views[viewNum]
                            view.toggle()
                            if (view.open) {
                                view.windowGroup.bringToTop(view)
                                view.controls.filter { viewControls.contains(it) }.forEach { it.open = true }
                            }
                        }
                    }

                    override fun update() {
                        if (GeometryHelper.intersects(Mouse.xPixel, Mouse.yPixel, 0, 0, ViewControlGUI.xPixel, ViewControlGUI.yPixel, ViewControlGUI.widthPixels, ViewControlGUI.heightPixels)) {
                            if ((Mouse.xPixel - this.xPixel) % (VIEW_SELECTOR_BUTTON_WIDTH + 2) > 5) {
                                viewHighlighted = -1; return
                            }
                            val viewNum = (Mouse.xPixel - this.xPixel) / (VIEW_SELECTOR_BUTTON_WIDTH + 2)
                            viewHighlighted = viewNum
                        } else
                            viewHighlighted = -1
                    }

                    override fun render() {
                        var i = 0
                        for (v in views) {
                            if (i != viewHighlighted)
                                Renderer.renderTexture(if (v.open) Image.GUI.VIEW_SELECTOR_CLOSE_BUTTON else Image.GUI.VIEW_SELECTOR_OPEN_BUTTON, ViewControlGUI.xPixel + i * 7, ViewControlGUI.yPixel)
                            else
                                Renderer.renderTexture(if (v.open) Image.GUI.VIEW_SELECTOR_CLOSE_BUTTON_HIGHLIGHT else Image.GUI.VIEW_SELECTOR_OPEN_BUTTON_HIGHLIGHT, ViewControlGUI.xPixel + i * 7, ViewControlGUI.yPixel)
                            i++
                        }
                    }
                }
        )
        viewControls.add(rootChild)
    }
}

object IngameGUI : GUIWindow("In game gui",
        { 0 }, { 0 },
        { Game.WIDTH }, { Game.HEIGHT },
        windowGroup = ScreenManager.Groups.BACKGROUND
), ControlPressHandler {

    const val MAX_VIEWS = 4

    val mainInvGUI = InventoryGUI("In game main inventory gui", "Inventory", Game.mainInv, 20, 20, layer = MAX_VIEWS * 3 + 2)

    val cameras = arrayOf<LevelObject>(newCamera(), newCamera(), newCamera(), newCamera())
    val views = arrayOf<ViewWindow>(newView(), newView(), newView(), newView())

    init {
        InputManager.registerControlPressHandler(this, ControlPressHandlerType.GLOBAL, Control.TOGGLE_VIEW_CONTROLS)
        adjustDimensions = true
        GUITexturePane(this.rootChild, "In game gui background", { 0 }, { 0 }, Image.GUI.MAIN_MENU_BACKGROUND_FILLER, { Game.WIDTH }, { Game.HEIGHT })
        ViewControlGUI.open
    }

    private fun newCamera(): Camera {
        val c = Camera(Game.currentLevel.widthPixels / 2, Game.currentLevel.heightPixels / 2)
        Game.currentLevel.add(c)
        return c
    }

    private fun newView(): ViewWindow {
        if (viewCount < MAX_VIEWS) {
            return ViewWindow("View ${++viewCount} window",
                    0, 0,
                    Game.WIDTH, Game.HEIGHT,
                    cameras[viewCount - 1],
                    open = viewCount == 1,
                    windowGroup = ScreenManager.Groups.VIEW).run {
                if (open)
                    Game.currentLevel.openViews.add(view)
                controls.forEach {
                    it.matchParentOpening = false
                    it.open = viewControlsOpen
                    viewControls.add(it)
                }
                return@run this
            }
        }
        throw Exception("GUI views exceeds limit")
    }

    override fun onDimensionChange(oldWidth: Int, oldHeight: Int) {
        for (v in views) {
            // If there are any fullscreen views, keep them fullscreen
            if (v.widthPixels == oldWidth && v.heightPixels == oldHeight) {
                v.widthAlignment = { widthPixels }
                v.heightAlignment = { heightPixels }
            }
        }
    }

    override fun onClose() {
        views.forEach { it.open = false }
        viewControlsOpen = false
        viewControls.forEach { it.open = false }
    }

    override fun handleControlPress(p: ControlPress) {
        if (p.control == Control.TOGGLE_VIEW_CONTROLS && open && p.pressType == PressType.PRESSED) {
            viewControlsOpen = !viewControlsOpen
            viewControls.filter { it !is GUIElement || it.parentWindow.open }.forEach { it.toggle() }
        }
    }
}
