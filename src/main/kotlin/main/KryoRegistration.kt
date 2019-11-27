package main

import audio.AudioManager
import behavior.BehaviorTree
import behavior.BehaviorTreeSerializer
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.SerializerFactory
import com.esotericsoftware.kryo.serializers.DefaultSerializers
import crafting.Recipe
import crafting.RecipeSerializer
import fluid.FluidTank
import fluid.FluidType
import fluid.MoltenOreFluidType
import graphics.*
import item.*
import item.weapon.WeaponItemType
import level.*
import level.block.*
import level.entity.Entity
import level.entity.EntityType
import level.entity.robot.BrainRobot
import level.entity.robot.Robot
import level.entity.robot.RobotType
import level.generator.*
import level.moving.MovingObject
import level.moving.MovingObjectType
import level.pipe.PipeBlock
import level.pipe.PipeState
import level.pipe.PipeStateSerializer
import level.tile.*
import level.tube.TubeBlock
import level.tube.TubeState
import level.tube.TubeStateSerializer
import network.User
import network.packet.*
import player.Player
import resource.*
import routing.RoutingLanguageSerializer
import routing.RoutingLanguageStatement
import routing.TubeRoutingNetwork
import java.util.*

fun registerKryo(kryo: Kryo) {
    with(kryo) {
        isRegistrationRequired = false
        references = true

        val clazz = java.util.Collections::class.java.declaredClasses.first { it.simpleName == "SynchronizedRandomAccessList" }
        register(clazz).setInstantiator { Collections.synchronizedList<Any?>(mutableListOf()) }

        register(TextureRegion::class.java)

        setDefaultSerializer(SerializerFactory.TaggedFieldSerializerFactory())

        register(PacketType::class.java, DefaultSerializers.EnumSerializer(PacketType::class.java))
        register(Packet::class.java)
        register(ClientHandshakePacket::class.java)
        register(ServerHandshakePacket::class.java)
        register(LevelInfoPacket::class.java)
        register(LevelDataPacket::class.java)
        register(LevelData::class.java)
        register(ChunkDataPacket::class.java)
        register(ChunkData::class.java)
        register(RequestLevelDataPacket::class.java)
        register(RequestLevelInfoPacket::class.java)
        register(RequestChunkDataPacket::class.java)
        register(PlaceBlockPacket::class.java)
        register(GenericPacket::class.java)

        register(User::class.java)
        register(LevelInfo::class.java)
        register(Chunk::class.java).setInstantiator { Chunk(0, 0) }
        register(Level::class.java, LevelSerializer())
        register(RemoteLevel::class.java, LevelSerializer())
        register(ActualLevel::class.java, LevelSerializer())
        register(LevelType::class.java, LevelTypeSerializer())
        register(LevelGenerator::class.java)

        register(ImageCollection::class.java)
        register(Animation::class.java)

        register(AudioManager.SoundSource::class.java)

        register(LevelObjectType::class.java, LevelObjectTypeSerializer())
        register(BlockType::class.java, LevelObjectTypeSerializer())
        register(MovingObjectType::class.java, LevelObjectTypeSerializer())
        register(EntityType::class.java, LevelObjectTypeSerializer())
        register(RobotType::class.java, LevelObjectTypeSerializer())

        setDefaultSerializer(LevelObjectSerializerFactory())
        register(LevelObject::class.java)
        register(Block::class.java)
        register(MovingObject::class.java)
        register(DroppedItem::class.java)
        register(Entity::class.java)
        register(Robot::class.java)
        register(ChestBlock::class.java)
        register(CrafterBlock::class.java)
        register(DefaultBlock::class.java)
        register(FluidTankBlock::class.java)
        register(FurnaceBlock::class.java)
        register(MachineBlock::class.java)
        register(MinerBlock::class.java)
        register(SolidifierBlock::class.java)
        register(PipeBlock::class.java)
        register(PipeState::class.java, PipeStateSerializer())
        register(TubeBlock::class.java)
        register(TubeState::class.java, TubeStateSerializer())

        setDefaultSerializer(SerializerFactory.TaggedFieldSerializerFactory())
        register(LevelObjectTextures::class.java).setInstantiator { LevelObjectTextures(Image.Misc.ERROR) }
        register(Renderable::class.java)
        register(Texture::class.java)

        register(Hitbox::class.java)

        register(Recipe::class.java, RecipeSerializer())
        register(ResourceNode::class.java)
        register(ResourceContainerGroup::class.java)
        register(ResourceContainer::class.java)
        register(ResourceList::class.java)

        register(Inventory::class.java)
        register(FluidTank::class.java)
        register(Item::class.java)
        register(ResourceType::class.java, ResourceTypeSerializer())
        register(ItemType::class.java, ResourceTypeSerializer())
        register(FluidType::class.java, ResourceTypeSerializer())
        register(MoltenOreFluidType::class.java, ResourceTypeSerializer())
        register(BlockItemType::class.java, ResourceTypeSerializer())
        register(WeaponItemType::class.java, ResourceTypeSerializer())
        register(IngotItemType::class.java, ResourceTypeSerializer())
        register(OreItemType::class.java, ResourceTypeSerializer())
        register(EntityItemType::class.java, ResourceTypeSerializer())

        register(BehaviorTree::class.java, BehaviorTreeSerializer())

        register(TileType::class.java, TileTypeSerializer())
        register(OreTileType::class.java, TileTypeSerializer())

        register(Tile::class.java)
        register(OreTile::class.java)

        register(TubeRoutingNetwork::class.java)
        register(ResourceNodeBehavior::class.java)
        register(ResourceNodeBehavior.RoutingLanguageIORule::class.java)
        register(RoutingLanguageStatement::class.java, RoutingLanguageSerializer())

        register(RequestPlayerDataPacket::class.java)
        register(PlayerDataPacket::class.java)
        register(Player::class.java)

        register(BrainRobot::class.java)
    }
}