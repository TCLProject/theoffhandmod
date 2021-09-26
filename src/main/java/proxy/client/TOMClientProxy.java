package proxy.client;

import java.util.List;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import mods.battlegear2.api.core.BattlegearUtils;
import mods.battlegear2.api.core.InventoryPlayerBattle;
import mods.battlegear2.api.shield.IShield;
import mods.battlegear2.client.BattlegearClientEvents;
import mods.battlegear2.client.BattlegearClientTickHandeler;
import mods.battlegear2.client.utils.BattlegearClientUtils;
import mods.battlegear2.packet.BattlegearAnimationPacket;
import mods.battlegear2.packet.SpecialActionPacket;
import mods.battlegear2.utils.EnumBGAnimations;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.tclproject.theoffhandmod.TheOffhandMod;
import proxy.server.TOMServerProxy;

public class TOMClientProxy extends TOMServerProxy {
   public static KeyBinding keyBindReverseAction;
   public static IIcon[] backgroundIcon;

    @Override
	public void registerHandlers() {}

	@Override
	public World getClientWorld() 
	{
		return FMLClientHandler.instance().getClient().theWorld;
	}

   @Override
   public void registerTickHandelers() 
   {
       super.registerTickHandelers();
       MinecraftForge.EVENT_BUS.register(BattlegearClientEvents.INSTANCE);
       FMLCommonHandler.instance().bus().register(BattlegearClientTickHandeler.INSTANCE);
       BattlegearUtils.RENDER_BUS.register(new BattlegearClientUtils());
   }

   @Override
   public void sendAnimationPacket(EnumBGAnimations animation, EntityPlayer entityPlayer) 
   {
       if (entityPlayer instanceof EntityClientPlayerMP) {
           ((EntityClientPlayerMP) entityPlayer).sendQueue.addToSendQueue(
                   new BattlegearAnimationPacket(animation, entityPlayer).generatePacket());
       }
   }

   @Override
   public void startFlash(EntityPlayer player, float damage) 
   {
   	if(player.getCommandSenderName().equals(Minecraft.getMinecraft().thePlayer.getCommandSenderName())){
           BattlegearClientTickHandeler.resetFlash();
           ItemStack offhand = ((InventoryPlayerBattle)player.inventory).getCurrentOffhandWeapon();

           if(offhand != null && offhand.getItem() instanceof IShield)
               BattlegearClientTickHandeler.reduceBlockTime(((IShield) offhand.getItem()).getDamageDecayRate(offhand, damage));
       }
   }

   @Override
   public IIcon getSlotIcon(int index)
   {
       if(backgroundIcon != null){
           return backgroundIcon[index];
       }else{
           return null;
       }
   }

   @Override
   public void doSpecialAction(EntityPlayer entityPlayer, ItemStack itemStack) 
   {
       MovingObjectPosition mop = null;
       if(itemStack != null && itemStack.getItem() instanceof IShield){
           mop = getMouseOver(1, 4);
       }

       FMLProxyPacket p;
       if(mop != null && mop.entityHit instanceof EntityLivingBase){
           p = new SpecialActionPacket(entityPlayer, mop.entityHit).generatePacket();
           if(mop.entityHit instanceof EntityPlayerMP){
        	   TheOffhandMod.packetHandler.sendPacketToPlayer(p, (EntityPlayerMP) mop.entityHit);
           }
       }else{
           p = new SpecialActionPacket(entityPlayer, null).generatePacket();
       }
       TheOffhandMod.packetHandler.sendPacketToServer(p);
   }

   @Override
   public EntityPlayer getClientPlayer()
   {
       return Minecraft.getMinecraft().thePlayer;
   }
   
   @Override
   public EntityPlayer getPlayerEntity(MessageContext ctx) 
   {
	   return Minecraft.getMinecraft().thePlayer;
   }

   /**
    * Finds what block or object the mouse is over at the specified partial tick time. Args: partialTickTime
    */
   @Override
   public MovingObjectPosition getMouseOver(float tickPart, float maxDist)
   {
       Minecraft mc = FMLClientHandler.instance().getClient();
       if (mc.renderViewEntity != null)
       {
           if (mc.theWorld != null)
           {
               mc.pointedEntity = null;
               double d0 = maxDist;
               MovingObjectPosition objectMouseOver = mc.renderViewEntity.rayTrace(d0, tickPart);
               double d1 = d0;
               Vec3 vec3 = mc.renderViewEntity.getPosition(tickPart);

               if (objectMouseOver != null)
               {
                   d1 = objectMouseOver.hitVec.distanceTo(vec3);
               }

               Vec3 vec31 = mc.renderViewEntity.getLook(tickPart);
               Vec3 vec32 = vec3.addVector(vec31.xCoord * d0, vec31.yCoord * d0, vec31.zCoord * d0);
               Entity pointedEntity = null;
               float f1 = 1.0F;
               List list = mc.theWorld.getEntitiesWithinAABBExcludingEntity(mc.renderViewEntity, mc.renderViewEntity.boundingBox.addCoord(vec31.xCoord * d0, vec31.yCoord * d0, vec31.zCoord * d0).expand(f1, f1, f1));
               double d2 = d1;

               for (Object element : list) {
                   Entity entity = (Entity)element;

                   if (entity.canBeCollidedWith())
                   {
                       float f2 = entity.getCollisionBorderSize();
                       AxisAlignedBB axisalignedbb = entity.boundingBox.expand(f2, f2, f2);
                       MovingObjectPosition movingobjectposition = axisalignedbb.calculateIntercept(vec3, vec32);

                       if (axisalignedbb.isVecInside(vec3))
                       {
                           if (0.0D < d2 || d2 == 0.0D)
                           {
                               pointedEntity = entity;
                               d2 = 0.0D;
                           }
                       }
                       else if (movingobjectposition != null)
                       {
                           double d3 = vec3.distanceTo(movingobjectposition.hitVec);

                           if (d3 < d2 || d2 == 0.0D)
                           {
                               pointedEntity = entity;
                               d2 = d3;
                           }
                       }
                   }
               }

               if (pointedEntity != null && (d2 < d1 || objectMouseOver == null))
               {
                   objectMouseOver = new MovingObjectPosition(pointedEntity);
               }

               return objectMouseOver;
           }
       }
       return null;
   }

   @Override
   public void register() 
   {
      super.register();
	  if (TOMClientProxy.keyBindReverseAction == null) {
          TOMClientProxy.keyBindReverseAction = new KeyBinding("Reverse Action", Keyboard.KEY_LMENU, "key.categories.gameplay");
          ClientRegistry.registerKeyBinding(TOMClientProxy.keyBindReverseAction);
          KeyBinding.resetKeyBindingArrayAndHash();
      }
   }

	@Override
	public Entity getEntityByID(World world, int ID)
	{
		return world.getEntityByID(ID);
	}

	@Override
	public EntityLivingBase getEntityByID(int entityID)
	{
		Entity e = Minecraft.getMinecraft().theWorld.getEntityByID(entityID);
		if (e instanceof EntityLivingBase) return (EntityLivingBase)e;
		return null;
	}

	@Override
	public WorldServer[] getWorldServers()
	{
		return FMLClientHandler.instance().getServer().worldServers;
	}

	@Override
	public boolean isClientPlayer(EntityLivingBase ent)
	{
		return ent instanceof AbstractClientPlayer;
	}
}
