package main

import io.OutputManager as out
import java.awt.Canvas
import java.awt.Dimension
import javax.swing.JFrame
import javax.swing.text.AttributeSet

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
    const val ASPECT_RATIO = 16 * 9
    const val HEIGHT: Int = WIDTH / ASPECT_RATIO
    const val SCALE = 3
    /* Thread */
    val THREAD = Thread(this, "Powerworks")
    /* Frame and update rates */
    const val UPDATES_PER_SECOND = 60f
    const val MS_PER_UPDATE: Float = 1000 / UPDATES_PER_SECOND
    const val NS_PER_UPDATE: Float = 1000000000 / UPDATES_PER_SECOND
    const val MAX_UPDATES_BEFORE_RENDER = 5
    var FRAMES_PER_SECOND = 60f
    var MS_PER_FRAME: Float = 1000 / FRAMES_PER_SECOND
    var NS_PER_FRAME: Float = 1000000000 / FRAMES_PER_SECOND
    /* Base statistics */
    var framesCount = 0
    var updatesCount = 0
    var secondsCount = 0
    var running = false

    val frame: JFrame = JFrame()

    fun init() {
        preferredSize = Dimension(WIDTH * SCALE, HEIGHT * SCALE)
        frame.title = "Powerworks"
        frame.add(this)
        frame.pack()
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.setLocationRelativeTo(null)
        requestFocusInWindow()
        frame.isVisible = true
        start()
    }

    override fun run() {
        var lastUpdate: Float = System.nanoTime().toFloat()
        var lastFrame = lastUpdate
        var lastSecond = (lastUpdate / 1000000000).toInt()
        var frames = 0
        var updates = 0
        while(running) {
            var now: Float = System.nanoTime().toFloat()
            var updatesBeforeRender = 0
            while(now - lastUpdate > NS_PER_UPDATE && updatesBeforeRender < MAX_UPDATES_BEFORE_RENDER) {
                update()
                lastUpdate += NS_PER_UPDATE
                updates++
                updatesBeforeRender++
                updatesCount++
            }
            if(now - lastUpdate > NS_PER_UPDATE)
                lastUpdate = now - NS_PER_UPDATE
            render()
            framesCount++
            frames++
            lastFrame = now
            val thisSecond = (lastUpdate / 1000000000).toInt()
            if(thisSecond > lastSecond) {
                out.println("1 second: $updatesCount UPS, $framesCount FPS")
                secondsCount++
                lastSecond = thisSecond
                framesCount = 0
                updatesCount = 0
            }
            while(now - lastFrame < NS_PER_FRAME && now - lastUpdate < NS_PER_UPDATE) {
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

    }

    fun render() {

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

}