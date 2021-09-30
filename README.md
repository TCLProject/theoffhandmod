# The Offhand Mod

The Offhand Mod is a modification of the [Mine&Blade Battlegear 2](https://github.com/Mine-and-blade-admin/Battlegear2) mod for Minecraft Forge 1.7.10 that aims to change it's dual wielding mechanics to add a permanently playable and usable offhand mechanic to the game.

## Changes

- Reworked controls:
  * Left click always triggers the main hand action, right click always triggers the offhand action. The action is swinging for a weapon, shooting for a bow, eating for food, placing for block, etc.
  * Pressing left/right click and the Alt key (configurable) allows you to reverse an action and swing a bow, block a sword, etc.
  * The above two changes mean that you can use any item you want, in any hand.
- Added ability to mine blocks with the offhand.
- Added ability to use items/place blocks when the other hand is occupied with a usable item.
- Removed any and all limitations to what you can put in the offhand and in the mainhand that were present in battlegear2.
- Instead of two pairs of three slots, there are two pairs of four slots available.
- Changed rendering of the hotbar: instead of adding to it, the mod now replaces it.
- Instead of the added slots being completely separated, they are now interchangeable with the vanilla hotbar. As a consequence, removed the GUI that was previously required to place items in the offhand.
- Removed feature that turns off dual wielding (as turning it off would provide no actual gameplay benefit anymore: see above).

To get a feel of if you like these modifications or not, I would recommend trying them out in-game. I am also open to suggestions.

## Installation to Game

1. Install Minecraft 1.7.10 with the **latest** Forge version.
2. Download and move the latest jar file from this mod's releases into the mods folder.

## Setting up a Development Environment

You can set up a devevelopment environment by Gradle, with IntelliJIdea: `gradlew setupDecompWorkspace idea genIntellijRuns` or Eclipse: `gradlew setupDecompWorkspace eclipse` then import the project. Complete the running configurations with the following VM option: `-Dfml.coreMods.load=net.tclproject.mysteriumlib.asm.fixes.MysteriumPatchesFixLoaderO`

## TODO

- Separate the two hands' active slots to make them indepentent of each other

## Contributing
Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

## License
This mod is currently licensed under the GPL 3.0 license with an additional exception clause to allow linking with the Minecraft software.
