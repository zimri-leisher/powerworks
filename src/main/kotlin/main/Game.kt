package main

import audio.AudioManager
import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Cursor
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.TextureRegion
import data.FileManager
import data.FileSystem
import data.ResourceManager
import graphics.*
import graphics.text.TextManager
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
import org.lwjgl.openal.AL
import screen.IngameGUI
import screen.MainMenuGUI
import screen.ScreenManager
import screen.elements.GUICloseButton
import screen.mouse.Mouse
import screen.mouse.Tool
import screen.mouse.Tooltips
import java.io.File
import java.net.URL
import java.security.Policy

/* Utility extensions */
fun URL.toFileHandle() = Gdx.files.internal(path)
val TextureRegion.widthPixels: Int
    get() = regionWidth
val TextureRegion.heightPixels: Int
    get() = regionHeight
fun toColor(r: Int = 255, g: Int = 255, b: Int = 255, a: Int = 255) = Color(r / 255f, g / 255f, b / 255f, a / 255f)
fun toColor(r: Float = 1f, g: Float = 1f, b: Float = 1f, a: Float = 1f) = Color(r, g, b, a)
fun toColor(color: Int = 0xFFFFFF, alpha: Float = 1f): Color {
    val c = Color(color)
    c.a = alpha
    return c
}
fun Color.toWhite() = this.set(1f, 1f, 1f, 1f)

fun main(args: Array<String>) {
    val config = LwjglApplicationConfiguration()
    config.title = "Powerworks Industries"
    config.width = main.Game.WIDTH * main.Game.SCALE
    config.height = main.Game.HEIGHT * main.Game.SCALE
    config.foregroundFPS = main.Game.FRAMES_PER_SECOND
    config.backgroundFPS = main.Game.FRAMES_PER_SECOND
    LwjglApplication(Game, config)
}

object Game : ApplicationAdapter(), ControlPressHandler {

    /* Dimensions */
    var WIDTH = 300
    var HEIGHT = (WIDTH.toDouble() / 16 * 9).toInt()
    const val SCALE = 4

    /* Frame and update rates */
    const val UPDATES_PER_SECOND = 60
    const val NS_PER_UPDATE: Float = 1000000000f / UPDATES_PER_SECOND
    const val MAX_UPDATES_BEFORE_RENDER = 5
    var FRAMES_PER_SECOND = 60
    var NS_PER_FRAME: Float = 1000000000f / FRAMES_PER_SECOND

    /**
     * Frames since the beginning of this execution
     */
    var framesCount = 0
    /**
     * Updates since the beginning of this execution
     */
    var updatesCount = 0
    /**
     * Seconds since the beginning of this execution
     */
    var secondsCount = 0

    private lateinit var defaultGameCursor: Cursor

    val JAR_PATH = Game::class.java.protectionDomain.codeSource.location.toURI().path.drop(1)

    /**
     * The current debug code that is used for displaying miscellaneous information.
     * @see DebugCode
     */
    var currentDebugCode = DebugCode.NONE

    var LEVEL_PAUSED = false

    var PAUSE_LEVEL_IN_ESCAPE_MENU = false
    val INVENTORY_WIDTH = 8

    val INVENTOR_HEIGHT = 6

    lateinit var currentLevel: Level

    lateinit var mainInv: Inventory

    private var lastUpdateTime = System.nanoTime().toDouble()
    private var lastRenderTime = System.nanoTime().toDouble()
    private var lastSecondTime = (lastUpdateTime / 1000000000).toInt()
    private var frameCount = 0
    private var updateCount = 0

    override fun create() {
        // order matters with some of these!
        ResourceManager.registerAtlas("textures/atlas/all.atlas")
        defaultGameCursor = Gdx.graphics.newCursor(Pixmap(Gdx.files.internal("textures/cursor/cursor_default.png")), 0, 0)
        Image.Misc
        Image.GUI
        Image.Block
        Image.Fluid
        Image.Item
        Image.Particle
        ScreenManager
        TextManager
        FileManager
        Tool
        Tooltips
        AudioManager.load()
        Gdx.input.inputProcessor = InputManager
        Gdx.graphics.setCursor(defaultGameCursor)
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
    }

    override fun render() {
        val now = System.nanoTime().toDouble()
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
        renderFinal()
        framesCount++
        frameCount++
        lastRenderTime = now
        val thisSecond = (lastUpdateTime / 1000000000).toInt()
        if (thisSecond > lastSecondTime) {
            lastSecondTime = thisSecond
            secondsCount++
            println("$frameCount FPS, $updateCount UPS")
            frameCount = 0
            updateCount = 0
        }
    }

    fun update() {
        FileSystem.update()
        InputManager.update()
        Tooltips.update()
        Animation.update()
        ScreenManager.update()
        if (State.CURRENT_STATE == State.INGAME) {
            currentLevel.update()
        }
        State.update()
    }

    fun renderFinal() {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        Renderer.batch.begin()
        ScreenManager.render()
        Mouse.render()
        Renderer.batch.end()
    }

    override fun resize(width: Int, height: Int) {
        WIDTH = width / SCALE
        HEIGHT = height / SCALE
        ScreenManager.screenSizeChange()
        Renderer.batch.projectionMatrix.setToOrtho2D(0f, 0f, width.toFloat(), height.toFloat())
    }

    override fun dispose() {
        Renderer.batch.dispose()
        ResourceManager.dispose()
        TextManager.dispose()
        defaultGameCursor.dispose()
        AL.destroy()
        AudioManager.close()
        ModManager.shutdown()
        System.exit(0)
    }

    fun resetMouseIcon() {
        Gdx.graphics.setCursor(defaultGameCursor)
    }

    fun clearMouseIcon() {
        Gdx.graphics.setSystemCursor(com.badlogic.gdx.graphics.Cursor.SystemCursor.Arrow)
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
                    ScreenManager.openWindows.sortedBy { it.layer }.firstOrNull { window ->
                        window.windowGroup != ScreenManager.Groups.BACKGROUND && window.windowGroup != ScreenManager.Groups.VIEW &&
                                window.anyChild { it is GUICloseButton && it.open == true && it.actOn == window }
                    }?.open = false
                }
                Control.TOGGLE_INVENTORY -> {
                    if (State.CURRENT_STATE != State.INGAME)
                        return
                    IngameGUI.mainInvGUI.toggle()
                }
            }
    }
}