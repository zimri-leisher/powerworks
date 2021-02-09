package main

import audio.AudioManager
import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.TextureRegion
import data.FileManager
import data.FileSystem
import data.GameResourceManager
import graphics.Animation
import graphics.Renderer
import graphics.text.TextManager
import io.*
import level.LevelManager
import level.RemoteLevel
import network.ClientNetworkManager
import network.User
import network.packet.LoadGamePacket
import network.packet.Packet
import network.packet.PacketHandler
import network.packet.PacketType
import player.PlayerManager
import screen.ScreenManager
import screen.gui.GuiDebugInfo
import screen.gui.GuiDebugMenu
import screen.gui.GuiEscapeMenu
import screen.mouse.Mouse
import screen.mouse.Tooltips
import screen.mouse.tool.Tool
import serialization.Registration
import serialization.Serialization
import setting.Settings
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis

/* Utility extensions */
fun Class<*>.isKotlinClass(): Boolean {
    return this.declaredAnnotations.any {
        // hacky but should be safe for the foreseeable future
        it.annotationClass.qualifiedName == "kotlin.Metadata"
    }
}

fun <K, V> MutableMap<K, V>.removeIfKey(selector: (K) -> Boolean) = removeIf { selector(it.key) }

fun <K, V> MutableMap<K, V>.removeIfValue(selector: (V) -> Boolean) = removeIf { selector(it.value) }
fun <K, V> MutableMap<K, V>.removeIf(selector: (MutableMap.MutableEntry<K, V>) -> Boolean) {
    val iterator = this.iterator()
    for (entry in iterator) {
        if (selector(entry)) {
            iterator.remove()
        }
    }
}

val TextureRegion.width: Int
    get() = regionWidth
val TextureRegion.height: Int
    get() = regionHeight

fun toColor(r: Int = 255, g: Int = 255, b: Int = 255, a: Int = 255) = Color(r / 255f, g / 255f, b / 255f, a / 255f)
fun toColor(r: Float = 1f, g: Float = 1f, b: Float = 1f, a: Float = 1f) = Color(r, g, b, a)
fun toColor(color: Int = 0xFFFFFF, alpha: Float = 1f): Color {
    val c = Color()
    c.a = alpha
    c.r = (color and 0x00ff0000).ushr(16) / 255f
    c.g = (color and 0x0000ff00).ushr(8) / 255f
    c.b = (color and 0x000000ff) / 255f
    return c
}

fun <K, V> Map<K, V>.joinToString() = toList().joinToString()

const val SERVER_IP = "127.0.0.1"
const val SERVER_PORT = 9412

fun main(args: Array<String>) {
    //brain=мозг
    // разбомбить = fire
    // weapon = оружие
    // пуля = bullet
    // success = успех
    // впасть в депрессию
    // напасть = less concrete version of war
    //
    val config = Lwjgl3ApplicationConfiguration()
    Game.processArguments(args)
    config.setWindowedMode(Game.WIDTH * Game.SCALE, Game.HEIGHT * Game.SCALE)
    config.setWindowIcon("textures/icon_windows.png")
    config.setIdleFPS(Game.FRAMES_PER_SECOND / 3)
    config.setTitle("Powerworks Industries")
    config.useVsync(true)
    try {
        Lwjgl3Application(Game, config)
    } catch (t: Throwable) {
        t.printStackTrace(System.err)
        exitProcess(-1)
    }
    exitProcess(1)
}

object Game : ApplicationAdapter(), ControlEventHandler, PacketHandler {

    /* Dimensions */
    var WIDTH = 300
    var HEIGHT = (WIDTH.toDouble() / 16 * 9).toInt()
    val SCALE = 4

    /* Frame and update rates */
    const val UPDATES_PER_SECOND = 60
    const val NS_PER_UPDATE: Float = 1000000000f / UPDATES_PER_SECOND
    const val MAX_UPDATES_BEFORE_RENDER = 5
    var FRAMES_PER_SECOND = 60
    var NS_PER_FRAME: Float = 1000000000f / FRAMES_PER_SECOND

    private val OS = OperatingSystem.get()

    // temporary random user generation
    val source = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    val USER = User(OS.getUUID(), System.getProperty("user.name")) /*
            User(UUID.randomUUID(), Random().ints(5, 0, source.length)
            .asSequence()
            .map(source::get)
            .joinToString(""))
            */

    val VERSION = Version.`0_5_0`

    var IS_SERVER = false
        private set

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

    val JAR_PATH = Game::class.java.protectionDomain.codeSource.location.toURI().path.drop(1)

    /**
     * The current debug code that is used for displaying miscellaneous information.
     * @see DebugCode
     */
    var currentDebugCode = DebugCode.NONE

    fun processArguments(args: Array<String>) {
        if ("server" in args) {
            IS_SERVER = true
        }
    }

    private var lastUpdateTime = System.nanoTime().toDouble()
    private var lastRenderTime = System.nanoTime().toDouble()
    private var lastSecondTime = (lastUpdateTime / 1000000000).toInt()
    private var frameCount = 0
    private var updateCount = 0

    override fun create() {
        // order matters with some of these!
        GameResourceManager.registerAtlas("textures/all.atlas")
        Registration.registerAll()
        Serialization.warmup()
        if (!Settings.tryLoad("default")) {
            Settings.save("default")
        }
        ClientNetworkManager.start()
        ClientNetworkManager.registerServerPacketHandler(this, PacketType.LOAD_GAME)
        AudioManager.load()
        Gdx.input.inputProcessor = InputManager
        InputManager.register(this, Control.PIPE_INFO, Control.TURN_OFF_DEBUG_INFO, Control.TAKE_SCREENSHOT, Control.POSITION_INFO, Control.RESOURCE_NODES_INFO, Control.RENDER_HITBOXES, Control.SCREEN_INFO, Control.CHUNK_INFO, Control.TUBE_INFO)
        GuiDebugMenu.open = false
        GuiEscapeMenu.open = false
        GuiDebugInfo.open = true
        GameState.setState(GameState.MAIN_MENU)
    }

    override fun render() {
        val now = System.nanoTime().toDouble()
        var updates = 0
        while (now - lastUpdateTime > NS_PER_UPDATE && updates < MAX_UPDATES_BEFORE_RENDER) {
            val updateTime = measureTimeMillis { update() }
            if (updateTime > 6) {
                //println("ms to update: $updateTime")
            }
            lastUpdateTime += NS_PER_UPDATE
            updates++
            updateCount++
            updatesCount++
        }
        if (now - lastUpdateTime > NS_PER_UPDATE) {
            lastUpdateTime = now - NS_PER_UPDATE
        }
        if (!IS_SERVER) {
            renderFinal()
            framesCount++
            frameCount++
            lastRenderTime = now
        }
        val thisSecond = (lastUpdateTime / 1000000000).toInt()
        if (thisSecond > lastSecondTime) {
            lastSecondTime = thisSecond
            secondsCount++
            //println("$frameCount FPS, $updateCount UPS")
            frameCount = 0
            updateCount = 0
        }
    }

    fun update() {
        FileSystem.update()
        ClientNetworkManager.update()
        InputManager.update()
        Mouse.update()
        Tooltips.update()
        Animation.update()
        ScreenManager.update()
        if (GameState.currentState == GameState.INGAME) {
            LevelManager.update()
        }
        Tool.update()
        Tool.update()
        GameState.update()
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
        if (IS_SERVER) return
        WIDTH = width / SCALE
        HEIGHT = height / SCALE
        ScreenManager.onScreenSizeChange()
        Renderer.batch.projectionMatrix.setToOrtho2D(0f, 0f, width.toFloat(), height.toFloat())
    }

    override fun dispose() {
        Settings.save("default")
        InputManager.shutdown()
        ClientNetworkManager.close()
        Renderer.batch.dispose()
        GameResourceManager.dispose()
        TextManager.dispose()
        AudioManager.close()
        System.exit(0)
    }

    override fun handleControlEvent(event: ControlEvent) {
        if (event.type == ControlEventType.PRESS)
            when (event.control) {
                Control.TURN_OFF_DEBUG_INFO -> currentDebugCode = DebugCode.NONE
                Control.TAKE_SCREENSHOT -> FileManager.takeScreenshot()
                Control.RENDER_HITBOXES -> currentDebugCode = DebugCode.RENDER_HITBOXES
                Control.CHUNK_INFO -> currentDebugCode = DebugCode.CHUNK_INFO
                Control.RESOURCE_NODES_INFO -> currentDebugCode = DebugCode.RESOURCE_NODES_INFO
                Control.PIPE_INFO -> currentDebugCode = DebugCode.PIPE_INFO
                Control.TUBE_INFO -> currentDebugCode = DebugCode.TUBE_INFO
                Control.SCREEN_INFO -> currentDebugCode = DebugCode.SCREEN_INFO
                Control.POSITION_INFO -> currentDebugCode = DebugCode.POSITION_INFO
            }
    }

    override fun handleClientPacket(packet: Packet) {
    }

    override fun handleServerPacket(packet: Packet) {
        if (packet is LoadGamePacket) {
            PlayerManager.localPlayer = packet.localPlayer
            val localLevel = RemoteLevel(packet.localPlayer.homeLevelId, packet.currentLevelInfo)
            localLevel.initialize()
            localLevel.load()
        }
    }
}