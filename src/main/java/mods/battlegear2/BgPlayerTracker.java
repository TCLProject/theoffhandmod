package mods.battlegear2;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import mods.battlegear2.packet.LoginPacket;
import net.minecraft.entity.player.EntityPlayerMP;
import net.tclproject.theoffhandmod.TheOffhandMod;

public final class BgPlayerTracker {

    public static final BgPlayerTracker INSTANCE = new BgPlayerTracker();

    private BgPlayerTracker(){}

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if(event.player instanceof EntityPlayerMP){
        	TheOffhandMod.packetHandler.sendPacketToPlayer(new LoginPacket().generatePacket(), (EntityPlayerMP)event.player);
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if(FMLCommonHandler.instance().getEffectiveSide().isClient())
        	TheOffhandMod.battlegearEnabled = false;
    }
}
