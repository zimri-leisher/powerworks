package main

import audio.AudioManager
import audio.Sound
import graphics.Font
import graphics.Image.GUI
import graphics.LocalAnimation
import graphics.Renderer
import graphics.SyncAnimation
import inv.Inventory
import inv.Item
import inv.ItemType
import io.*
import level.Level
import screen.*
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import javax.imageio.ImageIO
import javax.swing.JFrame
import io.OutputManager as out

const val TRACE_GRAPHICS = false

fun main(args: Array<String>) {
    initializeProperties()
    Game
    val `in` = Scanner(System.`in`)
    var s: String? = null
    while (`in`.hasNext()) {
        s = `in`.nextLine()
        val split = s!!.split(" ")
        val first = split[0]
        var second: String = "1"
        if (split.size > 1)
            second = split[1]
        val item = ItemType.ALL.firstOrNull { it.name.toLowerCase().equals(first.toLowerCase().replace("_", " ").trim()) }
        if (item != null)
            HUD.Hotbar.items.add(Item(item, second.toInt()))
        else
            println("not an item")
    }
}

fun initializeProperties() {
    if (TRACE_GRAPHICS)
        System.setProperty("sun.java2d.trace", "log,timestamp,count,help")
    System.setProperty("sun.java2d.opengl", "true")
    System.setProperty("sun.java2d.translaccel", "true")
    System.setProperty("sun.java2d.ddforcevram", "true")
}

object Game : Canvas(), Runnable, ControlPressHandler {

    val JAR_PATH = Game::class.java.protectionDomain.codeSource.location.toURI().path.substring(1 until Game::class.java.protectionDomain.codeSource.location.toURI().path.lastIndexOf("/"))

    /* Dimensions */
    var WIDTH = 300
    var HEIGHT = (WIDTH.toDouble() / 16 * 9).toInt()
    const val SCALE = 4

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
    private var running = false
    private var defaultCursor = Cursor.getDefaultCursor()
    private var clearCursor = Toolkit.getDefaultToolkit().createCustomCursor(ImageIO.read(Game::class.java.getResource("/textures/cursor/cursor_default.png")), Point(0, 0), "Blank cursor")

    /* Settings */
    var THREAD_WAITING = true
    var RENDER_HITBOXES = false
    var CHUNK_BOUNDARIES = false
    var LEVEL_PAUSED = false
    var PAUSE_LEVEL_IN_ESCAPE_MENU = false
    var DEBUG_TUBE_INFO = false
    val INVENTORY_WIDTH = 8
    val INVENTOR_HEIGHT = 6
    val RANDOM_CHANCE_OF_SPARK = 200

    var SPARK_ANIMATION = -1

    /* Level */
    lateinit var currentLevel: Level
    lateinit var mainInv: Inventory

    val frame: JFrame = JFrame()
    private var resized = false

    init {
        preferredSize = Dimension(WIDTH * SCALE, HEIGHT * SCALE)
        frame.title = "Powerworks"
        frame.add(this)
        frame.pack()
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.setLocationRelativeTo(null)
        requestFocusInWindow()
        frame.iconImage = ImageIO.read(Game::class.java.getResource("/textures/misc/logo.png"))
        frame.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {
                resized = true
            }
        })
        createData()
        addKeyListener(InputManager)
        addMouseWheelListener(InputManager)
        addMouseMotionListener(InputManager)
        addMouseListener(InputManager)
        AudioManager.load()
        cursor = clearCursor
        Font
        InputManager.registerControlPressHandler(this, ControlPressHandlerType.GLOBAL, Control.TAKE_SCREENSHOT, Control.TOGGLE_RENDER_HITBOXES, Control.TOGGLE_CHUNK_INFO, Control.TOGGLE_INVENTORY, Control.TOGGLE_DEBUG_TUBE_GROUP_INFO)
        /* For initializations (objects in Kotlin are loaded the first time they are called */
        MainMenuGUI.open = true
        DebugOverlay.open = false
        State.setState(State.MAIN_MENU)
        frame.isVisible = true
        start()
    }

    override fun run() {
        var lastUpdateTime = System.nanoTime().toDouble()
        var lastRenderTime: Double
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
        if (resized) {
            val oldW = Game.WIDTH
            val oldH = Game.HEIGHT
            Game.WIDTH = Game.width / SCALE
            Game.HEIGHT = Game.height / SCALE
            ScreenManager.screenSizeChange(oldW, oldH)
            resized = false
        }
        //spark()
        InputManager.update()
        Mouse.update()
        SyncAnimation.update()
        LocalAnimation.update()
        ScreenManager.update()
        if (State.CURRENT_STATE == State.INGAME) {
            if (updatesCount % 60 == 0)
                currentLevel.maxRenderSteps++
            currentLevel.update()
        }
        State.update()
    }

    /**
     * Fun little spark animation that copies motherlode
     */
    fun spark() {
        if ((Math.random() * RANDOM_CHANCE_OF_SPARK).toInt() == 0) {
            Sound.MOTHERLODE_SPARK.play()
            SPARK_ANIMATION = 0
        }
        if (SPARK_ANIMATION != -1) {
            val a = MainMenuGUI.logo
            SPARK_ANIMATION++
            if (SPARK_ANIMATION == 1) {
                a.texture == GUI.MAIN_MENU_LOGO_2
            } else if (SPARK_ANIMATION == 15) {
                a.texture = GUI.MAIN_MENU_LOGO_3
            } else if (SPARK_ANIMATION > 30) {
                SPARK_ANIMATION = -1
                a.texture = GUI.MAIN_MENU_LOGO
            }
        }
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
                Mouse.render()
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
        AudioManager.close()
    }

    fun resetMouseIcon() {
        cursor = defaultCursor
    }

    fun clearMouseIcon() {
        cursor = clearCursor
    }

    fun createData() {
        val controls = Paths.get(JAR_PATH, "data/settings/controls/")
        if (Files.notExists(controls))
            Files.createDirectories(controls)
        val defaultMap = Paths.get(JAR_PATH, "data/settings/controls/default.txt")
        if (Files.notExists(defaultMap)) {
            Files.createFile(defaultMap)
        }
        val f = defaultMap.toFile()
        f.writeText(Game::class.java.getResource("/settings/controls/default.txt").readText())
        val save = Paths.get(JAR_PATH, "data/save/")
        if (Files.notExists(save))
            Files.createDirectory(save)
    }

    // make it so that you place down blocks based ont he mouse held item, etc
    fun takeScreenshot() {
        val directory = Paths.get(JAR_PATH, "/screenshots/")
        if (Files.notExists(directory))
            Files.createDirectory(directory)
        val ss = graphicsConfiguration.createCompatibleImage(Game.WIDTH * Game.SCALE, Game.HEIGHT * Game.SCALE)
        Renderer.g2d = ss.createGraphics()
        ScreenManager.render()
        Renderer.g2d.dispose()
        val calInstance = Calendar.getInstance()
        val fileName = "${JAR_PATH}/screenshots/${calInstance.get(Calendar.MONTH) + 1}-${calInstance.get(Calendar.DATE)}-${calInstance.get(Calendar.YEAR)}"
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
                Control.TOGGLE_INVENTORY -> {
                    if (State.CURRENT_STATE != State.INGAME)
                        return
                    if (!ScreenManager.Groups.INVENTORY.windows.any { it.open }) {
                        IngameGUI.mainInvGUI.open = true
                    } else {
                        ScreenManager.Groups.INVENTORY.getTop { it.open }?.toggle()
                    }
                }
                Control.TOGGLE_DEBUG_TUBE_GROUP_INFO -> DEBUG_TUBE_INFO = !DEBUG_TUBE_INFO
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