package main

import graphics.Renderer
import io.*
import level.Level
import player.Player
import screen.DebugOverlay
import screen.MainMenuGUI
import screen.ScreenManager
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import javax.imageio.ImageIO
import javax.swing.JFrame
import io.OutputManager as out

const val TRACE_GRAPHICS = false

fun main(args: Array<String>) {
    initializeProperties()
    Game.poke()
}

fun initializeProperties() {
    if (TRACE_GRAPHICS)
        System.setProperty("sun.java2d.trace", "log,timestamp,count,help")
    System.setProperty("sun.java2d.opengl", "true")
    System.setProperty("sun.java2d.translaccel", "true")
    System.setProperty("sun.java2d.ddforcevram", "true")
}

object Game : Canvas(), Runnable, ControlPressHandler {

    val JAR_PATH = Game::class.java.protectionDomain.codeSource.location.toURI().path.substring(1).substring(0 until Game::class.java.protectionDomain.codeSource.location.toURI().path.lastIndexOf("/"))

    /* Dimensions */
    const val WIDTH = 300
    const val HEIGHT = (WIDTH.toFloat() / 16 * 9).toInt()
    const val SCALE = 4

    val THREAD = Thread(this, "Powerworks Main Thread")
    /* Frame and update rates */
    const val UPDATES_PER_SECOND = 60f
    const val NS_PER_UPDATE: Float = 1000000000 / UPDATES_PER_SECOND
    const val MAX_UPDATES_BEFORE_RENDER = 5
    var FRAMES_PER_SECOND = 60000000f
    var NS_PER_FRAME: Float = 1000000000 / FRAMES_PER_SECOND
    /* Base statistics */
    var framesCount = 0
    var updatesCount = 0
    var secondsCount = 0
    private var running = false
    private var defaultCursor = Cursor.getDefaultCursor()
    private var clearCursor = Toolkit.getDefaultToolkit().createCustomCursor(ImageIO.read(Game::class.java.getResource("/textures/cursor/cursor_default.png")), Point(0, 0), "Blank cursor")

    private val fonts = mutableMapOf<Int, Font>()
    private lateinit var defaultFont: Font

    /* Settings */
    var THREAD_WAITING = false
    var RENDER_HITBOXES = false
    var CHUNK_BOUNDARIES = false

    /* Level */
    lateinit var currentLevel: Level
    lateinit var player: Player

    val frame: JFrame = JFrame()

    init {
        preferredSize = Dimension(WIDTH * SCALE, HEIGHT * SCALE)
        frame.title = "Powerworks"
        frame.add(this)
        frame.pack()
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.setLocationRelativeTo(null)
        requestFocusInWindow()
        frame.iconImage = ImageIO.read(Game::class.java.getResource("/textures/misc/logo.png"))
        frame.isVisible = true
        frame.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {
                println("resized")
            }
        })
        addKeyListener(InputManager)
        addMouseWheelListener(InputManager)
        addMouseMotionListener(InputManager)
        addMouseListener(InputManager)
        cursor = clearCursor
        try {
            val font = Font.createFont(Font.TRUETYPE_FONT, Game::class.java.getResourceAsStream("/font/MunroSmall.ttf")).deriveFont(Font.PLAIN, 28f)
            val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
            ge.registerFont(font)
            defaultFont = font
            fonts.put(28, font)
        } catch (ex: FontFormatException) {
            ex.printStackTrace()
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
        InputManager.registerControlPressHandler(this, Control.TAKE_SCREENSHOT, Control.TOGGLE_RENDER_HITBOXES, Control.TOGGLE_CHUNK_INFO)
        /* For initializations */
        MainMenuGUI.poke()
        DebugOverlay.poke()
        State.setState(State.MAIN_MENU)
        start()
    }

    /* Lazy initialization makes this a requirement */
    fun poke() {}

    override fun run() {
        var lastUpdateTime = System.nanoTime().toDouble()
        var lastRenderTime = System.nanoTime().toDouble()
        var lastSecondTime = (lastUpdateTime / 1000000000).toInt()
        var frameCount = 0
        var updateCount = 0
        while (running) {
            var now = System.nanoTime().toDouble()
            var updates = 0
            while (now - lastUpdateTime > NS_PER_UPDATE && updates < MAX_UPDATES_BEFORE_RENDER) {
                update()
                lastUpdateTime += NS_PER_UPDATE
                updates++
                updateCount++
                updatesCount++
            }
            if (now - lastUpdateTime > NS_PER_UPDATE) {
                lastUpdateTime = now - NS_PER_UPDATE
            }
            render()
            framesCount++
            frameCount++
            lastRenderTime = now
            val thisSecond = (lastUpdateTime / 1000000000).toInt()
            if (thisSecond > lastSecondTime) {
                DebugOverlay.setInfo("FPS", frameCount.toString())
                DebugOverlay.setInfo("UPS", updateCount.toString())
                lastSecondTime = thisSecond
                secondsCount++
                frameCount = 0
                updateCount = 0
            }
            if (THREAD_WAITING)
                while (now - lastRenderTime < NS_PER_FRAME && now - lastUpdateTime < NS_PER_UPDATE) {
                    Thread.yield()
                    try {
                        Thread.sleep(1)
                    } catch (e: Exception) {
                    }
                    now = System.nanoTime().toDouble()
                }
        }
        stop()
    }

    fun update() {
        InputManager.update()
        ScreenManager.update()
        if (State.CURRENT_STATE == State.INGAME)
            currentLevel.update()
        State.update()
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

    fun getFont(size: Int): Font {
        var font = fonts.get(size)
        if (font != null)
            return font
        font = defaultFont.deriveFont(size.toFloat())
        fonts.put(size, font)
        return font
    }

    fun resetMouseIcon() {
        cursor = defaultCursor
    }

    fun clearMouseIcon() {
        cursor = clearCursor
    }

    fun takeScreenshot() {
        val directory = Paths.get(JAR_PATH, "screenshots")
        if (Files.notExists(directory))
            Files.createDirectory(directory)
        val ss = graphicsConfiguration.createCompatibleImage(Game.WIDTH * Game.SCALE, Game.HEIGHT * Game.SCALE)
        Renderer.g2d = ss.createGraphics()
        ScreenManager.render()
        Renderer.g2d.dispose()
        val calInstance = Calendar.getInstance()
        val fileName = "${JAR_PATH}screenshots/${calInstance.get(Calendar.MONTH)}-${calInstance.get(Calendar.DATE)}-${calInstance.get(Calendar.YEAR)}"
        var i = 0
        var file = File(fileName + " #$i.png")
        while (file.exists()) {
            i++
            file = File(fileName + " #$i.png")
        }
        ImageIO.write(ss, "png", file)
        println("Taken screenshot")
    }

    override fun handleControlPress(p: ControlPress) {
        if (p.pressType == PressType.PRESSED)
            when (p.control) {
                Control.TAKE_SCREENSHOT -> takeScreenshot()
                Control.TOGGLE_RENDER_HITBOXES -> RENDER_HITBOXES = !RENDER_HITBOXES
                Control.TOGGLE_CHUNK_INFO -> CHUNK_BOUNDARIES = !CHUNK_BOUNDARIES
            }
    }

    private fun getDeviceConfigurationString(gc: GraphicsConfiguration): String {
        return "Bounds: " + gc.bounds + "\n" +
                "Buffer Capabilities: " + gc.bufferCapabilities.toString() + "\n" +
                "   Back Buffer Capabilities: " + gc.bufferCapabilities.backBufferCapabilities.toString() + "\n" +
                "      Accelerated: " + gc.bufferCapabilities.backBufferCapabilities.isAccelerated + "\n" +
                "      True Volatile: " + gc.bufferCapabilities.backBufferCapabilities.isTrueVolatile + "\n" +
                "   Flip Contents: " + gc.bufferCapabilities.flipContents.toString() + "\n" +
                "   Front Buffer Capabilities: " + gc.bufferCapabilities.frontBufferCapabilities.toString() + "\n" +
                "      Accelerated: " + gc.bufferCapabilities.frontBufferCapabilities.isAccelerated + "\n" +
                "      True Volatile: " + gc.bufferCapabilities.frontBufferCapabilities.isTrueVolatile + "\n" +
                "   Is Full Screen Required: " + gc.bufferCapabilities.isFullScreenRequired + "\n" +
                "   Is MultiBuffer Available: " + gc.bufferCapabilities.isMultiBufferAvailable + "\n" +
                "   Is Page Flipping: " + gc.bufferCapabilities.isPageFlipping + "\n" +
                "Device: " + gc.device.toString() + "\n" +
                "   Available Accelerated Memory: " + gc.device.availableAcceleratedMemory + "\n" +
                "   ID String: " + gc.device.iDstring + "\n" +
                "   Type: " + gc.device.type + "\n" +
                "   Display Mode: " + gc.device.displayMode + "\n" +
                "Image Capabilities: " + gc.imageCapabilities.toString() + "\n" +
                "      Accelerated: " + gc.imageCapabilities.isAccelerated + "\n" +
                "      True Volatile: " + gc.imageCapabilities.isTrueVolatile + "\n"
    }

}