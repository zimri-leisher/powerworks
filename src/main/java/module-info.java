module powerworks {
    requires transitive java.desktop;
    requires kotlin.stdlib;
    requires kotlinx.coroutines.core;
    requires kotlin.stdlib.jdk8;
    requires AudioCue.SNAPSHOT;
    uses mod.Mod;
    exports audio;
    exports crafting;
    exports graphics;
    exports io;
    exports item;
    exports level;
    exports level.block;
    exports level.living;
    exports level.moving;
    exports level.tile;
    exports level.tube;
    exports main;
    exports misc;
    exports mod;
    exports resource;
    exports screen;
    exports screen.elements;
    exports weapon;
}