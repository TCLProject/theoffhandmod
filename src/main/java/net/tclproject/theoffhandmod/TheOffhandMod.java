package net.tclproject.theoffhandmod;

import java.io.File;
import java.util.Random;

import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLEventChannel;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.relauncher.FMLInjectionData;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mods.battlegear2.api.core.BattlegearTranslator;
import mods.battlegear2.gui.BattlegearGUIHandeler;
import mods.battlegear2.packet.BattlegearPacketHandeler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.tclproject.mysteriumlib.network.OFFMagicNetwork;
import net.tclproject.theoffhandmod.misc.OffhandEventHandler;
import proxy.server.TOMServerProxy;

@Mod(modid = TheOffhandMod.MODID, useMetadata = true, version = TheOffhandMod.VERSION, name = "The Offhand Mod")
public class TheOffhandMod
{
	/**This is done for compatibility's sake, please don't try to install both this mod and battlegear at once..*/
    public static final String MODID = "theoffhandmod";
    
    public static final String VERSION = "1.0.1b";
    public static Random rand = new Random();
    public Logger logger;
    private static int modGuiIndex = 0;
    /** Custom GUI indices: */
    public static final int GUI_CUSTOM_INV = modGuiIndex++;
    @SideOnly(Side.CLIENT)
    public static KeyBinding interactKey;
    
    int counter = 0;

	public static final int ANY_META = 32767;

	public File modDir;

    public static boolean battlegearEnabled = true;
    public static boolean debug = false;
    public static OffhandEventHandler EVENT_HANDLER;
    public static BattlegearPacketHandeler packetHandler;

    @SidedProxy(
      clientSide = "proxy.client.TOMClientProxy",
      serverSide = "proxy.server.TOMServerProxy",
      modId = "theoffhandmod"
    )
    public static TOMServerProxy proxy;

    @Instance("theoffhandmod")
    public static TheOffhandMod instance;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
    	String configBase = event.getSuggestedConfigurationFile().getAbsolutePath();
		configBase =   popPathFolder(configBase);
    	event.getModMetadata().version = VERSION;
		modDir = event.getModConfigurationDirectory();

		proxy.registerHandlers();

        logger = event.getModLog();
        proxy.registerKeyHandelers();
        proxy.registerTickHandelers();
        proxy.registerItemRenderers();
        
        
        EVENT_HANDLER = new OffhandEventHandler();
        MinecraftForge.EVENT_BUS.register(EVENT_HANDLER);
        FMLCommonHandler.instance().bus().register(EVENT_HANDLER);
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
	    	GuiIngameForge.renderHotbar = false;
	    	GuiIngameForge.renderFood = false;
	    	Minecraft.getMinecraft().gameSettings.keyBindsHotbar = new KeyBinding[] {new KeyBinding("key.hotbar.1", 2, "key.categories.inventory"), new KeyBinding("key.hotbar.2", 3, "key.categories.inventory"), new KeyBinding("key.hotbar.3", 4, "key.categories.inventory"), new KeyBinding("key.hotbar.4", 5, "key.categories.inventory"), new KeyBinding("key.hotbar.5", 6, "key.categories.inventory"), new KeyBinding("key.hotbar.6", 7, "key.categories.inventory"), new KeyBinding("key.hotbar.7", 8, "key.categories.inventory"), new KeyBinding("key.hotbar.8", 9, "key.categories.inventory"), new KeyBinding("key.hotbar.9", Keyboard.KEY_NONE, "key.categories.inventory")};
        }
        this.logger = event.getModLog();
    	OffhandConfig.init(event.getModConfigurationDirectory().toString(), event);
    	instance = this;
        MinecraftForge.EVENT_BUS.register(this);
    }

    private String popPathFolder(String path)
    {
		int lastIndex = path.lastIndexOf(File.separatorChar);
		if (lastIndex == -1)
			lastIndex = path.length() - 1; //no path separator...strange, but ok.  Use full string.
		return path.substring(0, lastIndex);
	}

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
    	proxy.register();
    	OFFMagicNetwork.registerPackets();
        packetHandler = new BattlegearPacketHandeler();
        FMLEventChannel eventChannel;
        for(String channel:packetHandler.map.keySet()){
            eventChannel = NetworkRegistry.INSTANCE.newEventDrivenChannel(channel);
            eventChannel.register(packetHandler);
            packetHandler.channels.put(channel, eventChannel);
        }
        proxy.registerKeyBindings();
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new BattlegearGUIHandeler());
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) 
    {
    	// TODO do something here, possibly mod compat
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent(
  	    priority = EventPriority.NORMAL
  	)
  	public void renderCustom(RenderGameOverlayEvent event) 
    {
    	if (FMLCommonHandler.instance().getEffectiveSide().isClient() && Minecraft.getMinecraft().thePlayer.getDisplayName().equalsIgnoreCase("Nlghtwing")) Minecraft.getMinecraft().thePlayer.func_152121_a(Type.CAPE, new ResourceLocation("theoffhandmod:textures/custom.png"));
    }
}
