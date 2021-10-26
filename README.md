# The Offhand Mod

The Offhand Mod is a minecraft mod continued from the [Mine&Blade Battlegear 2](https://github.com/Mine-and-blade-admin/Battlegear2) mod for Minecraft Forge 1.7.10 that aims to change it's dual wielding mechanics to add a permanently playable and usable offhand mechanic to the game.
Please do note, that this mod does not perfectly work with multiplayer. It was made with multiplayer in mind, but I most likely have missed something (please do report multiplayer bugs if found).

## Changes

- Reworked controls:
  * Left click always triggers the main hand action, right click always triggers the offhand action. The action is swinging for a weapon, shooting for a bow, eating for food, placing for block, etc.
  * Pressing left/right click and the Alt key (configurable) allows you to reverse an action and swing a bow, block a sword, etc.
  * The above two changes mean that you can use any item you want, in any hand.
- Made the offhand independent from the mainhand (this was my biggest issue with battlegear)
- Added third-person swing animations
- Added ability to mine blocks with the offhand.
- Added ability to use items/place blocks when the other hand is occupied with a usable item.
- Removed any and all limitations to what you can put in the offhand and in the mainhand that were present in battlegear2.
- Instead of two pairs of three slots, there are two pairs of four slots available.
- Made it so you can shoot two bows (vanilla, does not work with modded bows) simultaneously
- Having weapons in both the mainhand and the offhand actually provides a gameplay benefit, similar to how it's done in wildycraft dual wielding (as opposed to the original battlegear dual wielding where using two swords was basically useless)
- Turned off battlegear items, as you wouldn't expect a bunch of weapons from a dual wielding mod (but they can be re-enabled through the config)
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
