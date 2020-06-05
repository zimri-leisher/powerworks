package serialization

import audio.AudioManager
import audio.Sound
import behavior.Behavior
import behavior.BehaviorTree
import behavior.VariableData
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.esotericsoftware.kryonet.FrameworkMessage
import crafting.Recipe
import crafting.RecipeCategory
import data.*
import fluid.FluidTank
import fluid.FluidType
import fluid.MoltenOreFluidType
import graphics.*
import item.*
import item.tool.ToolItemType
import item.weapon.Projectile
import item.weapon.WeaponItem
import item.weapon.WeaponItemType
import level.*
import level.block.*
import level.entity.Entity
import level.entity.EntityType
import level.entity.robot.BrainRobot
import level.entity.robot.Robot
import level.entity.robot.RobotType
import level.generator.EmptyLevelGenerator
import level.generator.LevelGenerator
import level.generator.LevelType
import level.generator.OpenSimplexNoise
import level.moving.MovingObject
import level.moving.MovingObjectType
import level.particle.ParticleType
import level.pipe.FluidPipeBlock
import level.pipe.ItemPipeBlock
import level.pipe.PipeBlock
import level.pipe.PipeState
import level.tile.OreTile
import level.tile.OreTileType
import level.tile.Tile
import level.tile.TileType
import main.DebugCode
import main.Version
import misc.PixelCoord
import misc.TileCoord
import network.*
import network.packet.*
import player.*
import resource.*
import routing.*
import routing.script.*
import screen.Camera
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap
import kotlin.collections.LinkedHashSet
import kotlin.reflect.KClass
import kotlin.system.measureTimeMillis

object Registration {

    fun <R : Any> register(type: KClass<R>, serializer: Serializer<*>, id: Int) = type.register(id, serializer)
    fun <R : Any> register(type: KClass<R>, id: Int) = type.register(id)
    fun <R : Any> register(type: Class<R>, serializer: Serializer<*>, id: Int) = type.register(id, serializer)
    fun <R : Any> register(type: Class<R>, id: Int) = type.register(id)

    fun registerAll() {

        // max 230
        /* COLLECTIONS */
        val singletonList = listOf(1)
        register(singletonList::class, CollectionSerializer {it.toList()}, 209)
        register(emptyList<Nothing>()::class, EmptyListSerializer(), 160)
        val immutableListClass = Class.forName("java.util.Arrays${'$'}ArrayList") as Class<Collection<Any?>>
        register<Collection<Any?>>(immutableListClass, CollectionSerializer { listOf(*it.toTypedArray()) }, 141)
        register(ArrayList::class, MutableCollectionSerializer<ArrayList<Any?>>(), 142)
        register(LinkedHashSet::class, MutableCollectionSerializer<LinkedHashSet<Any?>>(), 143)
        register(Array<out Any?>::class, ArraySerializer(), 144)
        register(kotlin.Pair::class, PairSerializer(), 154)
        register(LinkedHashMap::class, MutableMapSerializer<LinkedHashMap<Any?, Any?>>(), 140)
        register(Set::class.java, MutableCollectionSerializer<LinkedHashSet<Any?>>(), 153)

        FrameworkMessage.RegisterTCP::class.register(147, Serializer.AllFields<FrameworkMessage.RegisterTCP>())
        FrameworkMessage.RegisterUDP::class.register(148, Serializer.AllFields<FrameworkMessage.RegisterUDP>())
        FrameworkMessage.KeepAlive::class.register(149, Serializer.AllFields<FrameworkMessage.KeepAlive>())
        FrameworkMessage.DiscoverHost::class.register(150, Serializer.AllFields<FrameworkMessage.DiscoverHost>())
        FrameworkMessage.Ping::class.register(151, Serializer.AllFields<FrameworkMessage.Ping>())
        register(Serialization.WarmupObject::class, Serializer.Tagged<Serialization.WarmupObject>(), 152)

        setSerializerFactory { Serializer.Tagged<Any>() }

        /* AUDIO */
        register(AudioManager.SoundSource::class, 110)
        register(Sound::class, EnumSerializer<Sound>(), 111)

        /* BEHAVIOR */
        Behavior
        register(BehaviorTree::class, IDSerializer({ BehaviorTree.ALL }, { it.id }), 112)
        register(VariableData::class, 113)

        /* CRAFTING */
        register(Recipe::class, IDSerializer({ Recipe.ALL }, { it.id }), 106)
        register(RecipeCategory::class, EnumSerializer<RecipeCategory>(), 107)

        /* DATA */
        register(ConcurrentlyModifiableMutableList::class, 114)
        register(ConcurrentlyModifiableMutableMap::class, 115)
        register(ConcurrentlyModifiableWeakMutableList::class, 116)
        register(ConcurrentlyModifiableWeakMutableMap::class, 117)
        register(WeakMutableList::class, 145)
        register(WeakMutableMap::class, 10)

        /* FLUID */
        register(FluidTank::class, 11)
        register(FluidType::class, IDSerializer({ FluidType.ALL }, { it.id }), 129)
        register(MoltenOreFluidType::class, IDSerializer({ MoltenOreFluidType.ALL }, { it.id }), 130)

        /* GRAPHICS */
        register(ImageCollection::class, 12)
        register(Renderable::class, 13)
        register(Texture::class, TextureSerializer(), 14)
        register(Animation::class, IDSerializer({ Animation.ALL }, { it.id }), 102)

        /* IO */

        /* ITEM */
        register(ToolItemType::class, IDSerializer({ ToolItemType.ALL }, { it.id }), 15)
        register(Projectile::class, 16)
        register(WeaponItem::class, 17)
        register(WeaponItemType::class, IDSerializer({ WeaponItemType.ALL }, { it.id }), 18)
        register(Inventory::class, 19)
        register(Item::class, 20)
        register(RobotItemType::class, IDSerializer({ RobotItemType.ALL }, { it.id }), 124)
        register(EntityItemType::class, IDSerializer({ EntityItemType.ALL }, { it.id }), 125)
        register(IngotItemType::class, IDSerializer({ IngotItemType.ALL }, { it.id }), 127)
        register(OreItemType::class, IDSerializer({ OreItemType.ALL }, { it.id }), 128)
        register(BlockItemType::class, IDSerializer({ BlockItemType.ALL }, { it.id }), 126)
        register(ItemType::class, IDSerializer({ ItemType.ALL }, { it.id }), 21)

        /* LEVEL */
        register(ActualLevel::class, LevelSerializer<ActualLevel>(), 52)
        register(Chunk::class, 53)
        register(DroppedItem::class, LevelObjectSerializer<DroppedItem>(), 54)
        register(Hitbox::class, 55)
        register(LevelData::class, 57)
        register(LevelInfo::class, 58)
        register(LevelObjectTextures::class, 60)
        register(RemoteLevel::class, LevelSerializer<RemoteLevel>(), 64)
        register(ChunkData::class, 146)
        register(GhostLevelObject::class, 201)
        register(AddObject::class, 222)
        register(RemoveObject::class, 223)
        register(DefaultLevelModification::class, 226)
        register(LevelModificationType::class, EnumSerializer<LevelModificationType>(), 227)
        register(SelectCrafterRecipe::class, 229)
        register(ModifyBrainRobotInv::class, 230)

        /* /BLOCK */
        register(BlockType::class, LevelObjectTypeSerializer(), 22)
        register(MachineBlockType::class, LevelObjectTypeSerializer(), 119)
        register(CrafterBlockType::class, LevelObjectTypeSerializer(), 120)
        register(FluidTankBlockType::class, LevelObjectTypeSerializer(), 121)
        register(ChestBlockType::class, LevelObjectTypeSerializer(), 122)
        register(PipeBlockType::class, LevelObjectTypeSerializer(), 188)

        setSerializerFactory { LevelObjectSerializer<LevelObject>() }

        register(Block::class, 23)
        register(ChestBlock::class, 24)
        register(CrafterBlock::class, 25)
        register(DefaultBlock::class, 26)
        register(FluidTankBlock::class, 27)
        register(FurnaceBlock::class, 28)
        register(MachineBlock::class, 29)
        register(MinerBlock::class, 30)
        register(SolidifierBlock::class, 31)
        register(PipeBlock::class, 189)
        register(RobotFactoryBlock::class, 217)

        /* /ENTITY */
        register(Entity::class, 32)
        register(EntityType::class, LevelObjectTypeSerializer(), 36)

        /* //ROBOT */
        register(BrainRobot::class, 33)
        register(Robot::class, 34)
        register(RobotType::class, LevelObjectTypeSerializer(), 35)

        /* /GENERATOR */
        setSerializerFactory { Serializer.Tagged<Any>() }
        register(EmptyLevelGenerator::class, 118)
        register(LevelGenerator::class, 37)
        register(LevelType::class, IDSerializer({ LevelType.ALL }, { it.id }), 38)
        register(OpenSimplexNoise::class, 40)

        /* /MOVING */
        register(MovingObject::class, LevelObjectSerializer<MovingObject>(), 41)
        register(MovingObjectType::class, LevelObjectTypeSerializer(), 42)

        /* /PARTICLE */
        register(ParticleType::class, IDSerializer({ ParticleType.ALL }, { it.id }), 43)

        /* /PIPE */
        register(FluidPipeBlock::class, LevelObjectSerializer<FluidPipeBlock>(), 44)
        register(ItemPipeBlock::class, LevelObjectSerializer<ItemPipeBlock>(), 186)
        register(PipeState::class, EnumSerializer<PipeState>(), 46)

        /* /TILE */
        register(OreTile::class, 47)
        register(OreTileType::class, IDSerializer({ OreTileType.ALL }, { it.id }), 108)
        register(Tile::class, 48)
        register(TileType::class, IDSerializer({ TileType.ALL }, { it.id }), 49)

        /* MAIN */
        register(DebugCode::class, EnumSerializer<DebugCode>(), 65)
        register(Version::class, EnumSerializer<Version>(), 66)

        /* MISC */
        register(PixelCoord::class, 67)
        register(TileCoord::class, 68)

        /* NETWORK */
        register(User::class, 69)

        /* /PACKET */
        register(ChunkDataPacket::class, 70)
        register(ClientHandshakePacket::class, 71)
        register(GenericPacket::class, 72)
        register(LevelDataPacket::class, 73)
        register(LevelInfoPacket::class, 74)
        register(Packet::class, 75)
        register(PacketType::class, EnumSerializer<PacketType>(), 77)
        register(PlayerDataPacket::class, 79)
        register(RequestChunkDataPacket::class, 80)
        register(RequestLevelDataPacket::class, 81)
        register(RequestLevelInfoPacket::class, 82)
        register(RequestPlayerDataPacket::class, 83)
        register(ServerHandshakePacket::class, 84)
        register(LoadGamePacket::class, 134)
        register(RequestLoadGamePacket::class, 135)
        register(PlayerActionPacket::class, 192)
        register(AcknowledgePlayerActionPacket::class, 196)
        register(BlockReference::class, NetworkReferenceSerializer(), 197)
        register(MovingObjectReference::class, NetworkReferenceSerializer(), 198)
        register(ResourceNodeReference::class, NetworkReferenceSerializer(), 199)
        register(DroppedItemReference::class, NetworkReferenceSerializer(), 216)
        register(LevelModificationPacket::class, 228)

        /* PLAYER */
        register(Player::class, 85)
        register(PlaceLevelObject::class, 202)
        register(RemoveLevelObjectAction::class, 203)
        register(SelectCrafterRecipeAction::class, 206)
        register(ControlEntityAction::class, 207)
        register(EditResourceNodeBehaviorAction::class, 208)
        register(PickUpDroppedItemAction::class, 215)

        /* RESOURCE */
        register(ResourceCategory::class, EnumSerializer<ResourceCategory>(), 86)
        register(ResourceContainer::class, 87)
        register(ResourceList::class, 89)
        register(ResourceNode::class, 90)
        register(ResourceNodeBehavior::class, 91)
        register(RoutingLanguageIORule::class, 92)
        register(ResourceType::class, IDSerializer({ ResourceType.ALL }, { it.id }), 93)

        /* ROUTING */
        register(Intersection::class, 94)
        register(Connections::class, 95)
        register(PackageRoute::class, 96)
        register(RouteStep::class, 97)
        register(ResourceRoutingNetwork::class, 98)
        register(RoutingLanguageStatement::class, RoutingLanguageStatementSerializer(), 99)
        register(ItemPipeRoutingNetwork::class, 100)
        register(PipeRoutingNetwork.PipeRoutingPackage::class, 101)
        register(FluidPipeRoutingNetwork::class, 187)

        register(TokenType::class.java, EnumSerializer<TokenType>(), 184)
        register(Token::class.java, 185)

        setSerializerFactory { NodeSerializer() }

        register(BooleanLiteral::class, 161)
        register(IntLiteral::class, 162)
        register(DoubleLiteral::class, 163)
        register(ResourceTypeLiteral::class, 164)
        register(TotalQuantity::class, 165)
        register(TotalNetworkQuantity::class, 166)
        register(Not::class, 167)
        register(QuantityOf::class, 168)
        register(NetworkQuantityOf::class, 169)
        register(Implies::class, 170)
        register(IfAndOnlyIf::class, 171)
        register(ExclusiveOr::class, 172)
        register(Or::class, 173)
        register(And::class, 174)
        register(Plus::class, 175)
        register(Minus::class, 176)
        register(Multiply::class, 177)
        register(Divide::class, 178)
        register(GreaterThan::class, 179)
        register(GreaterThanOrEqual::class, 180)
        register(LessThan::class, 181)
        register(LessThanOrEqual::class, 182)
        register(Equal::class, 183)

        setSerializerFactory { Serializer.Tagged<Any>() }

        /* SCREEN */
        register(Camera::class, LevelObjectSerializer<Camera>(), 131)

        val clazz = java.util.Collections::class.java.declaredClasses.first { it.simpleName == "SynchronizedRandomAccessList" }
        //register(clazz, 103).setInstantiator { Collections.synchronizedList<Any?>(mutableListOf()) }
        register(TextureRegion::class, 104)
        register(UUID::class, Serializer<UUID>(), 109).setSerializer({ UUID.fromString(it.readUTF()) }, { newInstance, _ -> newInstance }, { obj, output -> output.writeUTF(obj.toString()) })
        //register(Comparator::class, FieldSerializer<Comparator<*>>(this, Comparator::class), 132)
    }

    private var nextId = Primitive.values().maxBy { it.id }!!.id + 1

    val REFERENCE_ID = nextId++

    private val ids = mutableMapOf<Int, Class<*>>()
    private val registries = mutableMapOf<Class<*>, ClassRegistry<*>>()

    private var serializerFactory: (type: Class<*>) -> Serializer<*> = { Serializer<Any>() }

    init {
        ids.put(Primitive.NULL.id, Nothing::class.java)
        ids.put(Primitive.BYTE.id, java.lang.Byte::class.java)
        ids.put(Primitive.SHORT.id, java.lang.Short::class.java)
        ids.put(Primitive.INT.id, java.lang.Integer::class.java)
        ids.put(Primitive.LONG.id, java.lang.Long::class.java)
        ids.put(Primitive.FLOAT.id, java.lang.Float::class.java)
        ids.put(Primitive.DOUBLE.id, java.lang.Double::class.java)
        ids.put(Primitive.CHAR.id, java.lang.Character::class.java)
        ids.put(Primitive.BOOLEAN.id, java.lang.Boolean::class.java)
        ids.put(Primitive.STRING.id, java.lang.String::class.java)
    }


    fun setSerializerFactory(fac: (type: Class<*>) -> Serializer<*>) {
        serializerFactory = fac
    }

    fun resetSerializerFactory() {
        serializerFactory = { Serializer<Any>() }
    }

    fun registerClass(type: Class<*>, id: Int = nextId++, registry: ClassRegistry<*>) {
        if (Serialization.isPrimitive(id)) throw RegistrationException("Cannot reregister a primitive id ($id)")
        if (id < 0) throw RegistrationException("Cannot register an id less than 0 (tried to register $id)")
        if (Function::class.java.isAssignableFrom(type)) throw RegistrationException("Cannot register lambdas")
        if (registries.putIfAbsent(type, registry) != null) {
            throw RegistrationException("Cannot reregister $type)")
        } else {
            if (ids.putIfAbsent(id, type) != null) {
                throw RegistrationException("Cannot reregister id $id")
            }
        }
        // this line here will initialize the object if it is an object
        type.kotlin.objectInstance
        debugln("Registered class $type with id $id")
    }

    fun <R : Any> Class<R>.register(id: Int = nextId++, serializer: Serializer<*> = serializerFactory(this)): ClassRegistry<R> {
        val registry = ClassRegistry(this, serializer)
        registerClass(this, id, registry)
        return registry
    }

    fun <T : Any> KClass<T>.register(id: Int = nextId++, serializer: Serializer<*> = serializerFactory(this.java)) = this.java.register(id, serializer)

    fun getRegistry(type: Class<*>): ClassRegistry<*> {
        val actualType = Serialization.makeTypeNice(type)
        return registries.get(actualType) ?: throw Exception("Unregistered class $actualType")
    }

    fun getId(type: Class<*>): Int {
        val actualType = Serialization.makeTypeNice(type)
        val id = ids.filterValues { it == actualType }.keys.firstOrNull()
        if (id == null) {
            throw Exception("Unregistered class ${actualType.typeName}")
        }
        return id
    }

    fun getType(id: Int): Class<*>? {
        return ids.get(id)
    }

}

enum class Primitive {
    NULL,
    BYTE,
    SHORT,
    INT,
    LONG,
    FLOAT,
    DOUBLE,
    CHAR,
    BOOLEAN,
    STRING;

    val id get() = ordinal
}

fun test() {
    val byteOutput = ByteArrayOutputStream(32)
    val output = Output(byteOutput)
    //val level = ActualLevel(UUID.randomUUID(), LevelInfo(User(UUID.randomUUID(), ""), "", "", LevelType.DEFAULT_SIMPLEX, 10L))
    //val levelObj = newPlayer(User(UUID.randomUUID(), "")).brainRobot
    //val array = Inventory(3, 3)
    println("Now write new thing: ${measureTimeMillis {
        val statement = RoutingLanguage.parse("quantity IRON_ORE > 4")
        output.write(statement)
    }}")

    output.close()
    val input = Input(ByteArrayInputStream(byteOutput.toByteArray()))
    println("now read new thing: ${measureTimeMillis {
        val statement = input.readUnknown()
        println(statement)

    }}")
    input.close()
}
