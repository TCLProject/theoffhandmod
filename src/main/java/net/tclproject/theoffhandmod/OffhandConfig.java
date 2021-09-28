package net.tclproject.theoffhandmod;

import java.io.File;

import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.common.config.Configuration;

public class OffhandConfig {

	public static Configuration config;
	public static boolean btgearItems = false;

	public static String CATEGORY_EXTRA = "Extras";

	public static void init(String configDir, FMLPreInitializationEvent event) {

		FMLCommonHandler.instance().bus().register(new OffhandConfig());

		if (config == null) {
			File path = new File(configDir + "/" + "theoffhandmod.cfg");
			config = new Configuration(path);
			loadConfiguration();
		}
	}

	private static void loadConfiguration() {
		
		btgearItems = config.getBoolean("Battlegear 2 Blocks, Items and Enchants", CATEGORY_EXTRA, false, "Option to re-enable the items, blocks, enchants and other content battlegear adds.");

		if (config.hasChanged()) {
			config.save();
		}

	}

	@SubscribeEvent
	public void onConfigChange(ConfigChangedEvent event) {

		if (event.modID.equalsIgnoreCase("theoffhandmod")) {
			loadConfiguration();
		}

	}

	public static Configuration getConfiguration() {
		return config;
	}


}
