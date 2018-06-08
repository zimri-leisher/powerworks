package main

import audio.AudioManager
import data.FileManager
import data.FileSystem
import graphics.LocalAnimation
import graphics.Renderer
import graphics.SyncAnimation
import io.*
import item.IngotItemType
import item.Inventory
import item.ItemType
import item.OreItemType
import level.Level
import level.block.BlockType
import level.block.ChestBlockType
import level.block.CrafterBlockType
import level.block.MachineBlockType
import mod.ModManager
import mod.ModPermissionsPolicy
import screen.*
import screen.elements.GUICloseButton
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.security.Policy
import javax.imageio.ImageIO
import javax.swing.JFrame
import io.OutputManager as out
import graphics.text.TextManager
import misc.testTags

const val TRACE_GRAPHICS = false

fun main(args: Array<String>) {
    initializeProperties()
    Game
}

fun initializeProperties() {
    if (TRACE_GRAPHICS)
        System.setProperty("sun.java2d.trace", "log,timestamp,count,help")
    System.setProperty("sun.java2d.opengl", "true")
    System.setProperty("sun.java2d.translaccel", "true")
    System.setProperty("sun.java2d.ddforcevram", "true")
}

object Game : Canvas(), Runnable, ControlPressHandler {

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
    private var clearCursor = Toolkit.getDefaultToolkit().createCustomCursor(ImageIO.read(ResourceManager.getRawResource("/textures/cursor/cursor_default.png")), Point(0, 0), "Blank cursor")

    /* Settings */
    var THREAD_WAITING = true

    /**
     * The current debug code that is used for displaying miscellaneous information.
     * @see DebugCode
     */
    var currentDebugCode = DebugCode.NONE

    var LEVEL_PAUSED = false
    var PAUSE_LEVEL_IN_ESCAPE_MENU = false
    val INVENTORY_WIDTH = 8
    val INVENTOR_HEIGHT = 6

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
        frame.iconImage = ImageIO.read(ResourceManager.getRawResource("/textures/misc/logo.png"))
        frame.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {
                resized = true
            }
        })
        TextManager
        FileManager
        addKeyListener(InputManager)
        addMouseWheelListener(InputManager)
        addMouseMotionListener(InputManager)
        addMouseListener(InputManager)
        AudioManager.load()
        cursor = clearCursor

        InputManager.registerControlPressHandler(this, ControlPressHandlerType.GLOBAL, Control.PIPE_INFO, Control.ESCAPE, Control.TURN_OFF_DEBUG_INFO, Control.TAKE_SCREENSHOT, Control.POSITION_INFO, Control.RESOURCE_NODES_INFO, Control.RENDER_HITBOXES, Control.SCREEN_INFO, Control.CHUNK_INFO, Control.TOGGLE_INVENTORY, Control.TUBE_INFO)
        // the main menu GUI is by default open, but it won't get initialized till we call it somewhere
        MainMenuGUI
        // just making sure these are loaded before mods load
        ItemType
        IngotItemType
        OreItemType
        BlockType
        MachineBlockType
        CrafterBlockType
        ChestBlockType
        State.setState(State.MAIN_MENU)
        Policy.setPolicy(ModPermissionsPolicy())
        System.setSecurityManager(SecurityManager())
        ModManager.initialize()
        frame.isVisible = true
        testTags()
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
        FileSystem.update()
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
        ModManager.shutdown()
    }

    fun resetMouseIcon() {
        cursor = defaultCursor
    }

    fun clearMouseIcon() {
        cursor = clearCursor
    }

    override fun handleControlPress(p: ControlPress) {
        if (p.pressType == PressType.PRESSED)
            when (p.control) {
                Control.TURN_OFF_DEBUG_INFO -> currentDebugCode = DebugCode.NONE
                Control.TAKE_SCREENSHOT -> FileManager.takeScreenshot()
                Control.RENDER_HITBOXES -> currentDebugCode = DebugCode.RENDER_HITBOXES
                Control.CHUNK_INFO -> currentDebugCode = DebugCode.CHUNK_INFO
                Control.RESOURCE_NODES_INFO -> currentDebugCode = DebugCode.RESOURCE_NODES_INFO
                Control.PIPE_INFO -> currentDebugCode = DebugCode.PIPE_INFO
                Control.TUBE_INFO -> currentDebugCode = DebugCode.TUBE_INFO
                Control.SCREEN_INFO -> currentDebugCode = DebugCode.SCREEN_INFO
                Control.POSITION_INFO -> currentDebugCode = DebugCode.POSITION_INFO
                Control.ESCAPE -> {
                    ScreenManager.openWindows.sortedBy { it.layer }.firstOrNull { window -> window.windowGroup != ScreenManager.Groups.BACKGROUND && window.windowGroup != ScreenManager.Groups.VIEW &&
                        window.anyChild { it is GUICloseButton && it.open == true && it.actOn == window } }?.open = false
                }
                Control.TOGGLE_INVENTORY -> {
                    if (State.CURRENT_STATE != State.INGAME)
                        return
                    IngameGUI.mainInvGUI.toggle()
                }
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