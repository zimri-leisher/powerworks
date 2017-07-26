package main

import io.OutputManager as out
import javax.swing.JFrame
import io.InputManager
import java.awt.*
import graphics.Renderer
import java.awt.image.BufferedImage
import graphics.Images
import screen.ScreenManager

fun main(args: Array<String>) {
    initializeProperties()
    Game.init()
}

fun initializeProperties() {
    System.setProperty("sun.java2d.opengl", "true")
    System.setProperty("sun.java2d.translaccel", "true")
    System.setProperty("sun.java2d.ddforcevram", "true")
}

object Game : Canvas(), Runnable {

    /* Dimensions */
    const val WIDTH = 400
    const val HEIGHT = (WIDTH.toFloat() / 16 * 9).toInt()
    const val SCALE = 3

    val THREAD = Thread(this, "Powerworks Main Thread")
    /* Frame and update rates */
    const val UPDATES_PER_SECOND = 60f
    const val NS_PER_UPDATE: Float = 1000000000 / UPDATES_PER_SECOND
    const val MAX_UPDATES_BEFORE_RENDER = 5
    var FRAMES_PER_SECOND = 60f
    var NS_PER_FRAME: Float = 1000000000 / FRAMES_PER_SECOND
    /* Base statistics */
    var framesCount = 0
    var updatesCount = 0
    var secondsCount = 0

    var running = false

    var defaultCursor = Cursor.getDefaultCursor()
    var clearCursor = Toolkit.getDefaultToolkit().createCustomCursor(BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB), Point(0, 0), "null")

    /* Settings */
    var THREAD_WAITING = true

    val frame: JFrame = JFrame()

    fun init() {
        preferredSize = Dimension(WIDTH * SCALE, HEIGHT * SCALE)
        addKeyListener(InputManager)
        addMouseWheelListener(InputManager)
        addMouseMotionListener(InputManager)
        addMouseListener(InputManager)
        frame.title = "Powerworks"
        frame.add(this)
        frame.pack()
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.setLocationRelativeTo(null)
        requestFocusInWindow()
        frame.isVisible = true
        cursor = clearCursor
        start()
    }

    override fun run() {
        var lastUpdate: Float = System.nanoTime().toFloat()
        var lastFrame: Float
        var lastSecond = (lastUpdate / 1000000000).toInt()
        var frames = 0
        var updates = 0
        while (running) {
            var now: Float = System.nanoTime().toFloat()
            var updatesBeforeRender = 0
            while (now - lastUpdate > NS_PER_UPDATE && updatesBeforeRender < MAX_UPDATES_BEFORE_RENDER) {
                update()
                lastUpdate += NS_PER_UPDATE
                updates++
                updatesBeforeRender++
                updatesCount++
            }
            if (now - lastUpdate > NS_PER_UPDATE)
                lastUpdate = now - NS_PER_UPDATE
            render()
            framesCount++
            frames++
            lastFrame = now
            val thisSecond = (lastUpdate / 1000000000).toInt()
            if (thisSecond > lastSecond) {
                out.println("$updatesCount UPS, $framesCount FPS")
                secondsCount++
                lastSecond = thisSecond
                framesCount = 0
                updatesCount = 0
            }
            if (THREAD_WAITING)
                while (now - lastFrame < NS_PER_FRAME && now - lastUpdate < NS_PER_UPDATE) {
                    Thread.yield()
                    try {
                        Thread.sleep(1)
                    } catch(e: Exception) {
                        e.printStackTrace()
                    }
                    now = System.nanoTime().toFloat()
                }
        }
        stop()
    }

    fun update() {
        InputManager.update()
        ScreenManager.update()
    }

    fun render() {
        val bufferStrat = bufferStrategy
        if (bufferStrat == null) {
            createBufferStrategy(3)
            return
        }
        do {
            do {
                val g2d = bufferStrat.drawGraphics as Graphics2D
                Renderer.g2d = g2d
                /* Render */
                ScreenManager.render()
                g2d.dispose()
                bufferStrat.show()
            } while (bufferStrat.contentsRestored())
        } while (bufferStrat.contentsLost())
    }

    private fun start() {
        THREAD.start()
        running = true
    }

    private fun stop() {
        System.exit(0)
        try {
            THREAD.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    fun resetMouseIcon() {
        cursor = defaultCursor
    }

    fun clearMouseIcon() {
        cursor = clearCursor
    }

}