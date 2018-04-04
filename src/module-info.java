module powerworks {
    requires transitive kotlin.stdlib;
    requires transitive java.desktop;
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