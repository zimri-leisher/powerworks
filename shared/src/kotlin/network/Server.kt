package network

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.esotericsoftware.minlog.Log
import data.FileManager
import data.FileSystem
import data.ResourceManager
import graphics.Image
import graphics.text.TextManager
import level.LevelManager
import level.generator.LevelType
import level.tile.OreTileType
import level.tile.TileType
import main.Game
import main.State
import network.packet.*
import player.PlayerManager
import screen.mouse.Tool
import serialization.Registration
import serialization.Serialization
import serialization.SerializerDebugger
import serialization.test
import kotlin.system.measureTimeMillis

fun main(args: Array<String>) {
    val config = Lwjgl3ApplicationConfiguration()
    Game.processArguments(args + "server")
    config.useVsync(true)
    config.setInitialVisible(true)
    config.setTitle("Powerworks Server")
    Lwjgl3Application(Server, config)
}

object Server : ApplicationAdapter(), PacketHandler {

    private var lastUpdateTime = System.nanoTime().toDouble()
    private var lastSecondTime = (lastUpdateTime / 1000000000).toInt()
    private var frameCount = 0
    private var updateCount = 0

    override fun create() {
        // order matters with some of these!
        ResourceManager.registerAtlas("textures/all.atlas")
        Registration.registerAll()
        Serialization.warmup()
        Log.ERROR()
        ServerNetworkManager.start()
        ServerNetworkManager.registerClientPacketHandler(this, PacketType.REQUEST_LOAD_GAME, PacketType.GENERIC)
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
        println("Closing server")
        LevelManager.saveLevels()
        PlayerManager.savePlayers()
        ServerNetworkManager.close()
        ResourceManager.dispose()
        TextManager.dispose()
        System.exit(0)
    }

    override fun handleClientPacket(packet: Packet) {
        if (packet is RequestLoadGamePacket) {
            val player = PlayerManager.getPlayer(packet.forUser)
            ServerNetworkManager.sendToClient(LoadGamePacket(player, player.homeLevel.info), packet.connectionId)
        }
        if (packet is GenericPacket) {
        }
    }

    override fun handleServerPacket(packet: Packet) {
    }
}