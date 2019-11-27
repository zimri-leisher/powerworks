package network

import audio.AudioManager
import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.esotericsoftware.minlog.Log
import data.FileManager
import data.FileSystem
import data.ResourceManager
import graphics.Image
import graphics.Renderer
import graphics.text.TextManager
import level.LevelManager
import level.generator.LevelType
import level.tile.OreTileType
import level.tile.TileType
import main.Game
import main.State
import main.registerKryo
import player.Player
import screen.mouse.Tool
import kotlin.system.measureTimeMillis

fun main(args: Array<String>) {
    val config = Lwjgl3ApplicationConfiguration()
    Game.processArguments(args + "server")
    config.useVsync(true)
    config.setInitialVisible(false)
    config.setTitle("Powerworks Server")
    Lwjgl3Application(Server, config)
}

object Server : ApplicationAdapter() {

    val connectedUsers = mutableListOf<User>()
    val players = mutableListOf<Player>()

    private var lastUpdateTime = System.nanoTime().toDouble()
    private var lastSecondTime = (lastUpdateTime / 1000000000).toInt()
    private var frameCount = 0
    private var updateCount = 0

    fun getPlayerFromUser(user: User) = players.first { it.user == user }

    override fun create() {
        // order matters with some of these!
        ResourceManager.registerAtlas("textures/all.atlas")
        FileManager
        TileType
        OreTileType
        LevelType
        Log.set(Log.LEVEL_DEBUG)
        registerKryo(Game.KRYO)
        Image
        Tool
        ServerNetworkManager.start()
    }

    override fun render() {
        val now = System.nanoTime().toDouble()
        var updates = 0
        while (now - lastUpdateTime > Game.NS_PER_UPDATE && updates < Game.MAX_UPDATES_BEFORE_RENDER) {
            val updateTime = measureTimeMillis { update() }
            if (updateTime > 6) {
                //println("ms to update: $updateTime")
            }
            lastUpdateTime += Game.NS_PER_UPDATE
            updates++
            updateCount++
            Game.updatesCount++
        }
        if (now - lastUpdateTime > Game.NS_PER_UPDATE) {
            lastUpdateTime = now - Game.NS_PER_UPDATE
        }
        val thisSecond = (lastUpdateTime / 1000000000).toInt()
        if (thisSecond > lastSecondTime) {
            lastSecondTime = thisSecond
            Game.secondsCount++
            //println("$frameCount FPS, $updateCount UPS")
            frameCount = 0
            updateCount = 0
        }
    }

    fun update() {
        FileSystem.update()
        ServerNetworkManager.update()
        if (State.CURRENT_STATE == State.INGAME) {
            LevelManager.update()
        }
        State.update()
    }

    override fun dispose() {
        ServerNetworkManager.close()
        Renderer.batch.dispose()
        ResourceManager.dispose()
        TextManager.dispose()
        AudioManager.close()
        System.exit(0)
    }
}