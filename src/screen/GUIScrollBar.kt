package screen

import graphics.Images
import graphics.Renderer
import graphics.Texture
import io.InputManager
import io.PressType
import misc.GeometryHelper

interface Scrollable {
    fun onScrollbarMove()
    var viewHeightPixels: Int
    var maxHeightPixels: Int
}

/**
 * Width pixels is always 6, height defaults to 12 if lower than 12
 */
class GUIScrollBar(parent: GUIElement, name: String, xPixel: Int, yPixel: Int, heightPixels: Int) : GUIElement(parent, name, xPixel, yPixel, 6, heightPixels) {

    private var currentPos = 0
    private var scrollBarHeight: Int = 0
    private var dragging = false
    private var mYPixelPrev: Int = 0
    private var top: Texture = Images.GUI_SCROLL_BAR_TOP
    private var mid: Texture = Images.GUI_SCROLL_BAR_MIDDLE
    private var bottom: Texture = Images.GUI_SCROLL_BAR_BOTTOM
    private var scrollBar = arrayOf<Texture>(Images.GUI_SCROLL_BAR_UNHIGHLIGHT_TOP,
            Images.GUI_SCROLL_BAR_UNHIGHLIGHT_MIDDLE,
            Images.GUI_SCROLL_BAR_UNHIGHLIGHT_BOTTOM,
            Images.GUI_SCROLL_BAR_HIGHLIGHT_TOP,
            Images.GUI_SCROLL_BAR_HIGHLIGHT_MIDDLE,
            Images.GUI_SCROLL_BAR_HIGHLIGHT_BOTTOM,
            Images.GUI_SCROLL_BAR_CLICK_TOP,
            Images.GUI_SCROLL_BAR_CLICK_MIDDLE,
            Images.GUI_SCROLL_BAR_CLICK_BOTTOM)
    private var current = arrayOf<Texture>(scrollBar[0], scrollBar[1], scrollBar[2])

    init {
        onScrollableHeightChange()
    }

    override fun onMouseActionOn(type: PressType, xPixel: Int, yPixel: Int) {
        when (type) {
            PressType.PRESSED -> if (GeometryHelper.intersects(xPixel, yPixel, 1, 1, xPixel + 1, currentPos + yPixel + 1, 4, scrollBarHeight)) {
                dragging = true
                mYPixelPrev = yPixel
                setBarTexture(2)
            }
            PressType.RELEASED -> {
                if (mouseOn && GeometryHelper.intersects(xPixel, yPixel, 1, 1, xPixel + 1, currentPos + yPixel + 1, 4, scrollBarHeight))
                    setBarTexture(1)
                else
                    setBarTexture(0)
                dragging = false
            }
        }
    }

    private fun setBarTexture(t: Int) {
        current[0] = scrollBar[t * 3]
        current[1] = scrollBar[t * 3 + 1]
        current[2] = scrollBar[t * 3 + 2]
    }

    fun getCurrentPos(): Int {
        return currentPos
    }

    /**
     * Does not do anything if dragging already
     */
    fun setCurrentPos(pos: Int) {
        if (dragging)
            return
        currentPos = Math.min(heightPixels - 2 - scrollBarHeight, Math.max(pos, 0))
        (parent as Scrollable).onScrollbarMove()
    }

    override fun render() {
        Renderer.renderTexture(top, xPixel, yPixel)
        Renderer.renderTexture(mid, xPixel, yPixel + 2, 6, heightPixels - 4)
        Renderer.renderTexture(bottom, xPixel, yPixel + heightPixels - 2)
        Renderer.renderTexture(current[0], xPixel + 1, currentPos + 1 + yPixel)
        for (i in current[0].heightPixels..scrollBarHeight - current[2].heightPixels - 1)
            Renderer.renderTexture(current[1], xPixel + 1, i + currentPos + 1 + yPixel)
        Renderer.renderTexture(current[2], xPixel + 1, scrollBarHeight - current[2].heightPixels + yPixel + currentPos + 1)
        super.render()
    }

    override fun onMouseActionOff(type: PressType, xPixel: Int, yPixel: Int) {
        if (type == PressType.RELEASED) {
            setBarTexture(0)
            dragging = false
        }
    }

    override fun onMouseLeave() {
        super.onMouseLeave()
        if (!dragging)
            setBarTexture(0)
    }

    override fun update() {
        val mYPixel = InputManager.mouseYPixel
        val mXPixel = InputManager.mouseXPixel
        if (mouseOn && !dragging) {
            if (GeometryHelper.intersects(mXPixel, mYPixel, 1, 1, xPixel + 1, currentPos + yPixel + 1, 4, scrollBarHeight))
                setBarTexture(1)
            else
                setBarTexture(0)
        }
        if (mYPixel != mYPixelPrev && dragging) {
            if (yPixel + currentPos + (mYPixel - mYPixelPrev) + 1 + scrollBarHeight <= yPixel + heightPixels - 1 && yPixel + (currentPos + (mYPixel - mYPixelPrev)) >= yPixel) {
                currentPos += mYPixel - mYPixelPrev
                mYPixelPrev = mYPixel
                (parent as Scrollable).onScrollbarMove()
            }
        }
    }

    fun onScrollableHeightChange() {
        val p = parent as Scrollable
        scrollBarHeight = Math.min((p.viewHeightPixels.toDouble() / (if (p.maxHeightPixels == 0) 1 else p.viewHeightPixels).toDouble() * heightPixels.toDouble()).toInt(), heightPixels - 2)
    }

    fun getMaxPos(): Int {
        return heightPixels - 2 - scrollBarHeight
    }
}