package main

import audio.AudioManager
import behavior.BehaviorTree
import behavior.BehaviorTreeSerializer
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.SerializerFactory
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
import level.entity.robot.Robot
import level.entity.robot.RobotType
import level.moving.MovingObject
import level.moving.MovingObjectType
import level.pipe.PipeBlock
import level.pipe.PipeState
import level.pipe.PipeStateSerializer
import level.tube.TubeBlock
import level.tube.TubeState
import level.tube.TubeStateSerializer
import network.User
import network.packet.ClientHandshakePacket
import network.packet.Packet
import network.packet.PacketType
import network.packet.ServerHandshakePacket
import resource.*
import routing.RoutingLanguageSerializer
import routing.RoutingLanguageStatement
import routing.TubeRoutingNetwork

fun registerKryo(kryo: Kryo) {
    with(kryo) {
        setDefaultSerializer(SerializerFactory.TaggedFieldSerializerFactory())

        register(User::class.java)

        register(Level::class.java).setInstantiator { SimplexLevel(LevelInfo("", "", "", LevelGeneratorSettings(0, 0), 0)) }
        register(Chunk::class.java).setInstantiator { Chunk(LevelManager.emptyLevel, 0, 0) }

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
        register(ResourceNodeGroup::class.java)
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

        register(TubeRoutingNetwork::class.java)
        register(ResourceNodeBehavior::class.java)
        register(ResourceNodeBehavior.RoutingLanguageIORule::class.java)
        register(RoutingLanguageStatement::class.java, RoutingLanguageSerializer())

        setDefaultSerializer(SerializerFactory.FieldSerializerFactory())
        register(PacketType::class.java)
        register(Packet::class.java)
        register(ClientHandshakePacket::class.java)
        register(ServerHandshakePacket::class.java)
    }
}