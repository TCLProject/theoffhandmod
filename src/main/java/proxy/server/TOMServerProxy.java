package proxy.server;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.server.FMLServerHandler;
import mods.battlegear2.BattlegearTickHandeler;
import mods.battlegear2.BattlemodeHookContainerClass;
import mods.battlegear2.BgPlayerTracker;
import mods.battlegear2.WeaponHookContainerClass;
import mods.battlegear2.utils.EnumBGAnimations;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;

public class TOMServerProxy {
   public ModContainer mod;

   public void register() {
   }

   public void sendAnimationPacket(EnumBGAnimations animation, EntityPlayer entityPlayer) {}

   public IIcon getSlotIcon(int index) {return null;}

   public MovingObjectPosition getMouseOver(float i, float v) { return null; }

   public void registerItemRenderers() {
   }

   public void startFlash(EntityPlayer player, float damage) {
   }

   public void doSpecialAction(EntityPlayer entityPlayer, ItemStack item) {}

   public EntityPlayer getPlayerEntity(MessageContext ctx) {
	   return ctx.getServerHandler().playerEntity;
   }

   public void registerKeyHandelers() {}

   public void registerTickHandelers(){
       FMLCommonHandler.instance().bus().register(BattlegearTickHandeler.INSTANCE);
       FMLCommonHandler.instance().bus().register(BgPlayerTracker.INSTANCE);
       MinecraftForge.EVENT_BUS.register(BattlemodeHookContainerClass.INSTANCE);
       MinecraftForge.EVENT_BUS.register(WeaponHookContainerClass.INSTANCE);
   }

   public EntityPlayer getClientPlayer() {
       return null;
   }

   public void registerHandlers() {}

	public World getClientWorld() {
		return null;
	}

	public void registerKeyBindings() {}
	
	public void changeKeyBindings() {}

	public Entity getEntityByID(World world, int ID) {
		Entity ent = null;
		for (Object o : world.loadedEntityList){
			if (o instanceof EntityLivingBase){
				ent = (EntityLivingBase)o;
				if (ent.getEntityId() == ID){
					return ent;
				}
			}
		}
		return null;
	}

	public WorldServer[] getWorldServers() {
		return FMLServerHandler.instance().getServer().worldServers;
	}

	public EntityLivingBase getEntityByID(int entityID) {
		Entity ent = null;
		for (WorldServer ws : getWorldServers()){
			ent = ws.getEntityByID(entityID);
			if (ent != null){
				if (!(ent instanceof EntityLivingBase)) return null;
				else break;
			}
		}
		return (EntityLivingBase)ent;
	}

	public boolean isClientPlayer(EntityLivingBase ent) {
		return false;
	}
}
