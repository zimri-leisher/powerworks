# powerworks
2D top down factory-building RTS game in Kotlin. Main source is in shared/src/kotlin.

# General story:
Biological intelligence is a thing of the past. A species of AI is spreading throughout the cosmos, very successfully, and until now, totally alone. One day, a vast fleet of alien AI simply blinked into existence outside the orbits of every major AI center and annihilated all of civilization. You are an AI which has taken refuge from the fleet on an untouched planet on the frontiers of the galaxy.
# Start:
You spawn in to a level. You only control the camera with WASD, but you have spawned with a robot called the Brain (your AI, according to the lore). It is controllable with the mouse and various keyboard shortcuts. Being a factory AI from the company Powerworks Industries, you remember very limited things, all of which are centered on mass production and heavy industry. You also have a radar block, which quickly alerts you to an incoming enemy orbital drop force.

Directions appear on the screen, telling you how to defend yourself. Move the Brain to a nearby patch of iron ore, and command it to mine the ore, producing Iron Ore items. You place down the last block you spawned with, a Furnace, and put the items into there.

The Furnace takes in 2 ore and produces 1 ingot. Do this 4 times, the directions say, and then use the Brain to craft (rather slowly) a Stationary Defense Turret. Once you have placed it as the directions ask you to, the wave will land in a short amount of time.

The wave consists of 4 scouting robots and 2 standard general-purpose robots equipped with short-range weaponry. They immediately target your radar, knowing that if it is disabled you will be unable to see if they are receiving backup. Your turret puts up a good effort and kills them all, but the radar is guaranteed to die (it is set to die extremely quickly, and will explode automatically if it hasn’t been destroyed by the end of the attack).
	Now you have an objective: get that radar back online so that they can’t hit you by surprise.
# Mid game:
After some more tutorials on crafting a radar machine, you get it back, and see a vast fleet headed towards you, destined to come in waves.

These will come in predetermined times (probably, some could be based on certain goals being reached) and are essentially the single player campaign of the game. During these waves, you will be trying to construct a transport ship to attack the alien planet with. After the destruction of the last alien ship, you use its power source and data to fly your ship to the alien homeworld, and you mount an attack on it. If the attack is successful, you will steal the Farcaster, which allows communication with, and transport in, the multiverse (free-play is now unlocked). If it is unsuccessful, several more alien waves will be generated, along with another power source and data core, allowing you to keep on trying to attack until you win.
# End game:
Free-play can be against other players, or against AI. AI bases are generated (possibly scaling based on some algorithm taking into account how much stuff you have) and contain valuable technology in their Brain robots, which you can extract and put to use. This is the motivation to play multiplayer--to gain access to new technology.

Player bases are, of course, made by real people. They do not contain technology and instead are seen as points you must conquer in order to get to the next alien base (you are not allowed to choose entirely freely to which planet you can transport, due to interference from their Farcaster, according to lore). You must raid their base and destroy their Farcaster, disabling it and allowing you to move onward with your fleet.

However, Farcaster connections are two-way. When you open a portal into their universe, they get one into yours. This sets the scene for essentially a 1v1 RTS on two different levels: your planet and the opponent’s. Transport between them is slow and thus some time is spent preparing and establishing oneself on the enemy planet. The limitations of Transmitters (see below) will A) make attackers hard to find at first as planet wide Transmitter networks are hard to make and B) prevent quick scrambling to stop burgeoning attack bases.The first person to destroy the enemy Farcaster or Brain wins and their opponent’s robots will cease functioning and all power to their machines will be removed.

After the raid, all blocks that were destroyed by the enemy and not rebuilt will reappear (resource containers will remain unchanged to prevent duping). Units you leave on the enemy planet will be gone forever. The Farcaster will be unable to be used for a medium amount of time if it has been destroyed. If the Brain has been destroyed, severe penalties will be incurred, but there is no reward for the killer.
# More specific stuff:
*Robots*:
Robots come in several varieties. There are smaller ones, which have light, if any, weaponry. There are larger ones, which could have up to several turrets mounted on them, targeting multiple enemies simultaneously. Some are specifically for combat, some are for quick movement, which is necessary to get footholds because you can only construct blocks with a certain range of robots.

Robots are unable to function without a Transmitter block or robot that performs the same function nearby. Transmitters, in the lore, send commands from the Brain to the workers. Robots outside of a transmitter are unable to be controlled and will sit and do nothing. The intent of this is to limit how fast you can move and take over new places--you need to establish a presence, even if it is very small, first. Additionally, Transmitters can only rebroadcast from other Transmitters, meaning there will be a chan that can be disrupted and thus must be defended.

Farcasters, more specifically, are machines that allow the Transmitter signal to pass through the multiverse instantaneously and in all places, and thus allow real time communication with different worlds. They can also slowly transport matter between worlds. The matter transportation speed will be a primary means of limiting just how much you can send at another player.

Robots are the primary means of warfare, although it should be a reasonable strategy to construct defensive turrets on the outer layers to advance forward faster. Robots will require ammunition reloading periodically, with slower robots generally having more ammunition and faster ones having less. They will need to go to certain machines which will load them up if the machine has enough raw resources. The intent of this is to limit how much you can do when traveling the multiverse, because it will be much harder to transport blocks and thus to transport ammo factories.

*Power*:
Energy in Powerworks is about rate, not quantity. There is limitless energy due to ultra-advanced entropy-sapping technology in the core of each machine (this was what allowed the AI species to develop so far), but the rate at which it flows is limited. Thus, machines will always function at a minimum level, and for some machines, it may be pretty fast, but you can buff your machines by placing devices nearby that will increase the energy flow. Importantly, this only affects blocks. Robots are unable to be buffed in this way because they are already using their core’s maximum potential for energy flow (or so the lore will say).

*Defensive structures*:
Walls are for losers. Build bigass turrets. Still want walls? Make a wall out of bigass turrets.
The turrets in Powerworks will be universally large and slow-shooting. The changes between tiers will be damage or effect based. Some higher end versions will have multiple gunheads that can target different enemies simultaneously.

Additionally, above some weight class, turrets aren’t able to be placed like regular blocks which are able to be placed in a circular range around a unit. They instead will only be able to be placed AT the unit’s location, and the unit will join the turret to become its brain. Because these types of turrets have a unit brain, they will be manually controllable, meaning the turrets with slow fire rates but huge effect won’t waste their shots due to ineffective AI if you are directing them.

*Factory blocks*:
	Factory blocks take in inputs and do something with them to create an output. They include mining machines, automated crafters, and other devices to automate production. They will be highly configurable in their behavior, as you will be able to write custom input and output rules in a miniature scripting language. However, this will be intended for later game optimization, as by default, their behavior will be pretty general.
  
Crafting blocks will automate the creation of items, blocks and robots. Each type of crafter will specialize in a category: weapon crafters, ammo crafters, robot crafters, etc. Ingredients for crafting will be very simplistic: ingots, wire, circuits, and some other basic components will be able to create nearly everything in the game.
  
Tubes and pipes will transport items and fluids, respectively, between factory blocks. They will take into account the IO behavior specified by the scripting language, are unrestricted by distance, and take no power. Tubes have a restricted speed, but pipes are instantaneous, providing an advantage to transporting ores as molten ores.

Other factory blocks include furnaces, which take ore and create ingots, forges, which take ore and create molten ore, and solidifiers, which take molten ore and create ingots. It feels like there are more but I don’t know them yet.
# General ideas:
As you can see, it is a combination of a factory-builder with an RTS, and I think of the two, the RTS will be the more important and more visible component. The factory-building is more simplistic than games like Factorio, partially because everything is made out of raw or first-level ingredients, and partially because of the intelligence of the tube/pipe routing system. The RTS part will usually involve commanding troops in one or two areas max, while building occasional defensive structures and slowly pushing the line forwards into a base. Combat will favor the defending side, due to the difficulty of transporting objects across the Farcaster. It is possible staging planets will be added where attackers can collect units and prepare them for a larger scale attack. Multiple camera views at once allow easier control of specific battlefield sections.


See https://twitter.com/PowerworksGame for more regular updates
