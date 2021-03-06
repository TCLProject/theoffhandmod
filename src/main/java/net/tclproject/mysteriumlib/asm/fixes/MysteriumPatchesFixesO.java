package net.tclproject.mysteriumlib.asm.fixes;

import static cpw.mods.fml.common.Loader.isModLoaded;
import static net.minecraftforge.client.IItemRenderer.ItemRenderType.EQUIPPED_FIRST_PERSON;
import static net.minecraftforge.client.IItemRenderer.ItemRenderType.FIRST_PERSON_MAP;
import static net.tclproject.theoffhandmod.OffhandConfig.doubleBow;

import cpw.mods.fml.common.Optional;
import cpw.mods.fml.common.event.FMLInterModComms;
import cpw.mods.fml.common.registry.GameData;
import invtweaks.InvTweaksContainerManager;
import invtweaks.InvTweaksContainerSectionManager;
import invtweaks.InvTweaksObfuscation;
import invtweaks.api.container.ContainerSection;
import melonslise.locks.Locks;
import melonslise.locks.common.network.HandlerCheckPin;
import melonslise.locks.common.tileentity.TileEntityLockableBase;
import mods.battlegear2.packet.BattlegearSyncItemPacket;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.*;
import net.tclproject.theoffhandmod.TheOffhandMod;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mods.battlegear2.BattlemodeHookContainerClass;
import mods.battlegear2.api.core.BattlegearUtils;
import mods.battlegear2.api.core.IBattlePlayer;
import mods.battlegear2.api.core.InventoryPlayerBattle;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityMultiPart;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.boss.EntityDragonPart;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Items;
import net.minecraft.item.Item.ToolMaterial;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.potion.Potion;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ItemInWorldManager;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.stats.AchievementList;
import net.minecraft.stats.StatList;
import net.minecraft.util.DamageSource;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapData;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.tclproject.mysteriumlib.asm.annotations.EnumReturnSetting;
import net.tclproject.mysteriumlib.asm.annotations.Fix;
import net.tclproject.mysteriumlib.asm.annotations.ReturnedValue;
import net.tclproject.mysteriumlib.network.OFFMagicNetwork;
import net.tclproject.theoffhandmod.misc.OffhandEventHandler;
import net.tclproject.theoffhandmod.packets.InactiveHandSyncClient;
import net.tclproject.theoffhandmod.packets.InactiveHandSyncServer;
import proxy.client.TOMClientProxy;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class MysteriumPatchesFixesO {

	/**Whether we should override the default action of the item in hand*/
	public static boolean shouldNotOverride;
	/**Whether we have just overriden the minecraft method that gets called on right click to substitute the offhand item*/
	public static boolean leftclicked;
    private static boolean consecutiveCancel;

    @Fix(returnSetting=EnumReturnSetting.ALWAYS)
	public static boolean isPlayer(EntityPlayer p) {
		return false;
	}

	@Fix(returnSetting=EnumReturnSetting.ALWAYS)
	public static EntityItem dropOneItem(EntityPlayer p, boolean p_71040_1_)
    {
        ItemStack stack = ((InventoryPlayerBattle)p.inventory).getCurrentItem();
        if (stack == null)
        {
        	ItemStack offStack = ((InventoryPlayerBattle)p.inventory).getCurrentOffhandWeapon();
        	if (offStack == null) return null;
        	else if (offStack.getItem().onDroppedByPlayer(stack, p)) {
        		int offCount = p_71040_1_ && ((InventoryPlayerBattle)p.inventory).getCurrentOffhandWeapon() != null ? ((InventoryPlayerBattle)p.inventory).getCurrentOffhandWeapon().stackSize : 1;
        		int slot = ((InventoryPlayerBattle)p.inventory).currentItemInactive - InventoryPlayerBattle.OFFSET - 4;
        		ItemStack item = ((InventoryPlayerBattle)p.inventory).decrStackSize(slot, offCount);
        		return p.func_146097_a(item, false, true);
        	}
    		return null;
        }

        if (stack.getItem().onDroppedByPlayer(stack, p))
        {
            int count = p_71040_1_ && p.inventory.getCurrentItem() != null ? p.inventory.getCurrentItem().stackSize : 1;
            int slot = ((InventoryPlayerBattle)p.inventory).currentItem;
            slot = slot - InventoryPlayerBattle.OFFSET + 4;
            ItemStack item = ((InventoryPlayerBattle)p.inventory).decrStackSize(slot, count);
            return p.func_146097_a(item, false, true);
        }

        return null;
    }

	public static boolean hasMultipleUseTextures(Item i) {
		if (i instanceof ItemBow) return true; // TODO: List to be expanded
		return false;
	}

	@Fix(returnSetting=EnumReturnSetting.ON_TRUE)
	@SideOnly(Side.CLIENT)
	public static boolean func_147116_af(Minecraft mc) {
		if (Minecraft.getMinecraft().thePlayer.inventory.getCurrentItem() == null || (mc.objectMouseOver.typeOfHit == MovingObjectType.BLOCK && Minecraft.getMinecraft().thePlayer.inventory.getCurrentItem() != null && !(Minecraft.getMinecraft().thePlayer.inventory.getCurrentItem().getItem() instanceof ItemMonsterPlacer)) || !BattlegearUtils.usagePriorAttack(Minecraft.getMinecraft().thePlayer.inventory.getCurrentItem())) {
			shouldNotOverride = true;
			return false;
		}
		KeyBinding keyCode = Minecraft.getMinecraft().gameSettings.keyBindUseItem;
		KeyBinding.setKeyBindState(keyCode.getKeyCode(), true);
		KeyBinding.onTick(keyCode.getKeyCode());
		shouldNotOverride = true;
		leftclicked = true;
		return true;
	}
	
	@Fix(returnSetting=EnumReturnSetting.ON_TRUE)
	@SideOnly(Side.CLIENT)
	public static boolean clickBlock(PlayerControllerMP mp, int p_78743_1_, int p_78743_2_, int p_78743_3_, int p_78743_4_)
    {
		if (OffhandEventHandler.cancelone) {
			mp.resetBlockRemoving();
			return true;
		}
		return false;
    }
	
	@Fix(returnSetting=EnumReturnSetting.ON_TRUE)
	@SideOnly(Side.CLIENT)
	public static boolean addBlockHitEffects(EffectRenderer er, int x, int y, int z, MovingObjectPosition target)
    {
		if (OffhandEventHandler.cancelone) {
			return true;
		}
		return false;
    }
	
	@Fix(returnSetting=EnumReturnSetting.ON_TRUE)
	@SideOnly(Side.CLIENT)
	public static boolean onPlayerDamageBlock(PlayerControllerMP mp, int p_78759_1_, int p_78759_2_, int p_78759_3_, int p_78759_4_)
    {
		if (OffhandEventHandler.cancelone) {
			mp.resetBlockRemoving();
			return true;
		}
		return false;
    }

	/**Whether the offhand can use it's item right now. Prevents the other animation from playing when we're actually using the item not hitting with it*/
	private static boolean noAltHandUse;
	
	/**Whether we're rendering the item in the main hand right now*/
	public static boolean renderingItem2;

	@Fix
	@SideOnly(Side.CLIENT)
	public static void renderEquippedItems(RenderPlayer p, AbstractClientPlayer p_77029_1_, float p_77029_2_)
    {
		if (!MysteriumPatchesFixesO.leftclicked) noAltHandUse = true;
    }

	@Fix(returnSetting=EnumReturnSetting.ALWAYS)
	public static EnumAction getItemUseAction(ItemStack itmst)
    {
		if (noAltHandUse) {
			noAltHandUse = false;
			return EnumAction.none;
		}
		else return itmst.getItem().getItemUseAction(itmst);
    }

	@Fix(returnSetting=EnumReturnSetting.ON_TRUE)
	@SideOnly(Side.CLIENT)
	public static boolean renderItemInFirstPerson(ItemRenderer i, float p_78440_1_) 
	{
//		if (!MysteriumPatchesFixesO.leftclicked) {
			customRenderItemInFirstPerson(i, p_78440_1_);
			return true;
//		}
//		return false;
    }
	
	private static void enableStandardShading() {
		RenderHelper.enableStandardItemLighting();
	}
	
	/**Dirty hack to prevent random resetting of block removal (why does this even happen?!) when breaking blocks with the offhand.*/
	public static int countToCancel = 0;
	/**If we have hotswapped the breaking item with the one in offhand and should hotswap it back when called next*/
	public static boolean hotSwapped = false;
	
	@SideOnly(Side.CLIENT)
	@Fix(returnSetting=EnumReturnSetting.ON_TRUE)
	public static boolean resetBlockRemoving(PlayerControllerMP controller)
    {
		if (countToCancel > 0) {
			countToCancel--;
			return true;
		}
		else {
			if (MysteriumPatchesFixesO.hotSwapped) {
				Minecraft.getMinecraft().thePlayer.inventory.currentItem -= BattlemodeHookContainerClass.prevOffhandOffset;
	            Minecraft.getMinecraft().playerController.syncCurrentPlayItem();
	            MysteriumPatchesFixesO.hotSwapped = false;
			}
            int value = TOMClientProxy.getRemainingHighlightTicks();
            if (BattlemodeHookContainerClass.changedHeldItemTooltips && !(value > 0)) {
            	Minecraft.getMinecraft().gameSettings.heldItemTooltips = true;
            	BattlemodeHookContainerClass.changedHeldItemTooltips = false;
            }
			return false;
		}
    }
	
	@Fix(returnSetting=EnumReturnSetting.ALWAYS)
	public static void processHeldItemChange(NetHandlerPlayServer server, C09PacketHeldItemChange p_147355_1_)
    {
        if (p_147355_1_.func_149614_c() >= 0 && p_147355_1_.func_149614_c() < (InventoryPlayer.getHotbarSize() + InventoryPlayerBattle.OFFSET))
        {
        	server.playerEntity.inventory.currentItem = p_147355_1_.func_149614_c();
        	server.playerEntity.func_143004_u();
        }
        else
        {
        	System.out.println(server.playerEntity.getCommandSenderName() + " tried to set an invalid carried item " + p_147355_1_.func_149614_c());
        }
    }
	
	
	// Reflection way of getting the serverController inside NetServerPlayHandler. Not needed yet but might be needed in the future.
//	private static final MethodHandle fieldGet;
//	
//	static {
//	    Field field;
//		try {
//			field = NetHandlerPlayServer.class.getDeclaredField("field_147367_d");
//		    field.setAccessible(true);
//		    fieldGet = MethodHandles.publicLookup().unreflectGetter(field);
//		} catch (final Exception e) {
//			throw new RuntimeException("Failed to create fieldGet of serverController instance in static block.", e);
//		}
//	}
	
	@Fix
	@SideOnly(Side.CLIENT)
	public static void syncCurrentPlayItem(PlayerControllerMP pcmp)
    {
		OFFMagicNetwork.dispatcher.sendToServer(new InactiveHandSyncServer(pcmp.mc.thePlayer));
    }
	
	@Fix(insertOnExit=false)
	public static void writeEntityToNBT(EntityPlayer player, NBTTagCompound p_70014_1_)
    {
		p_70014_1_.setInteger("inactivehand", ((InventoryPlayerBattle)player.inventory).currentItemInactive);
    }
	
	@Fix(insertOnExit=false)
	public static void readEntityFromNBT(EntityPlayer player, NBTTagCompound p_70014_1_)
    {
		if (p_70014_1_.getInteger("inactivehand") > 153 && p_70014_1_.getInteger("inactivehand") < 158) {
			((InventoryPlayerBattle)player.inventory).currentItemInactive = p_70014_1_.getInteger("inactivehand");
		} else {
			((InventoryPlayerBattle)player.inventory).currentItemInactive = 154;
		}
    }
	
	@Fix(insertOnExit=true)
	public static void syncPlayerInventory(ServerConfigurationManager m, EntityPlayerMP player)
    {
		OFFMagicNetwork.dispatcher.sendTo(new InactiveHandSyncClient(player), player);
    }
	
	@Fix(insertOnExit=true)
	public static void initializeConnectionToPlayer(ServerConfigurationManager m, NetworkManager p_72355_1_, EntityPlayerMP player, NetHandlerPlayServer nethandlerplayserver)
    {
		OFFMagicNetwork.dispatcher.sendTo(new InactiveHandSyncClient(player), player);
    }

	
	public static float onGround2;
	
	@Fix
	@SideOnly(Side.CLIENT)
	public static void doRender(RendererLivingEntity l, EntityLivingBase p_76986_1_, double p_76986_2_, double p_76986_4_, double p_76986_6_, float p_76986_8_, float p_76986_9_)
    {
		if (p_76986_1_ instanceof EntityPlayer) {
			onGround2 = ((IBattlePlayer)p_76986_1_).getOffSwingProgress(p_76986_9_);
		}
    }
	
	@Fix(returnSetting=EnumReturnSetting.ALWAYS)
	@SideOnly(Side.CLIENT)
	public static void setRotationAngles(ModelBiped b, float p_78087_1_, float p_78087_2_, float p_78087_3_, float p_78087_4_, float p_78087_5_, float p_78087_6_, Entity p_78087_7_)
    {
        b.bipedHead.rotateAngleY = p_78087_4_ / (180F / (float)Math.PI);
        b.bipedHead.rotateAngleX = p_78087_5_ / (180F / (float)Math.PI);
        b.bipedHeadwear.rotateAngleY = b.bipedHead.rotateAngleY;
        b.bipedHeadwear.rotateAngleX = b.bipedHead.rotateAngleX;
        b.bipedRightArm.rotateAngleX = MathHelper.cos(p_78087_1_ * 0.6662F + (float)Math.PI) * 2.0F * p_78087_2_ * 0.5F;
        b.bipedLeftArm.rotateAngleX = MathHelper.cos(p_78087_1_ * 0.6662F) * 2.0F * p_78087_2_ * 0.5F;
        b.bipedRightArm.rotateAngleZ = 0.0F;
        b.bipedLeftArm.rotateAngleZ = 0.0F;
        b.bipedRightLeg.rotateAngleX = MathHelper.cos(p_78087_1_ * 0.6662F) * 1.4F * p_78087_2_;
        b.bipedLeftLeg.rotateAngleX = MathHelper.cos(p_78087_1_ * 0.6662F + (float)Math.PI) * 1.4F * p_78087_2_;
        b.bipedRightLeg.rotateAngleY = 0.0F;
        b.bipedLeftLeg.rotateAngleY = 0.0F;

        if (b.isRiding)
        {
            b.bipedRightArm.rotateAngleX += -((float)Math.PI / 5F);
            b.bipedLeftArm.rotateAngleX += -((float)Math.PI / 5F);
            b.bipedRightLeg.rotateAngleX = -((float)Math.PI * 2F / 5F);
            b.bipedLeftLeg.rotateAngleX = -((float)Math.PI * 2F / 5F);
            b.bipedRightLeg.rotateAngleY = ((float)Math.PI / 10F);
            b.bipedLeftLeg.rotateAngleY = -((float)Math.PI / 10F);
        }

        if (b.heldItemLeft != 0)
        {
            b.bipedLeftArm.rotateAngleX = b.bipedLeftArm.rotateAngleX * 0.5F - ((float)Math.PI / 10F) * (float)b.heldItemLeft;
        }

        if (b.heldItemRight != 0)
        {
            b.bipedRightArm.rotateAngleX = b.bipedRightArm.rotateAngleX * 0.5F - ((float)Math.PI / 10F) * (float)b.heldItemRight;
        }

        b.bipedRightArm.rotateAngleY = 0.0F;
        b.bipedLeftArm.rotateAngleY = 0.0F;
        float f6;
        float f7;

        if (b.onGround > -9990.0F)
        {
            f6 = b.onGround;
            b.bipedBody.rotateAngleY = MathHelper.sin(MathHelper.sqrt_float(f6) * (float)Math.PI * 2.0F) * 0.2F;
            b.bipedRightArm.rotationPointZ = MathHelper.sin(b.bipedBody.rotateAngleY) * 5.0F;
            b.bipedRightArm.rotationPointX = -MathHelper.cos(b.bipedBody.rotateAngleY) * 5.0F;
            b.bipedLeftArm.rotationPointZ = -MathHelper.sin(b.bipedBody.rotateAngleY) * 5.0F;
            b.bipedLeftArm.rotationPointX = MathHelper.cos(b.bipedBody.rotateAngleY) * 5.0F;
            b.bipedRightArm.rotateAngleY += b.bipedBody.rotateAngleY;
            b.bipedLeftArm.rotateAngleY += b.bipedBody.rotateAngleY;
            b.bipedLeftArm.rotateAngleX += b.bipedBody.rotateAngleY;
            f6 = 1.0F - b.onGround;
            f6 *= f6;
            f6 *= f6;
            f6 = 1.0F - f6;
            f7 = MathHelper.sin(f6 * (float)Math.PI);
            float f8 = MathHelper.sin(b.onGround * (float)Math.PI) * -(b.bipedHead.rotateAngleX - 0.7F) * 0.75F;
            b.bipedRightArm.rotateAngleX = (float)((double)b.bipedRightArm.rotateAngleX - ((double)f7 * 1.2D + (double)f8));
            b.bipedRightArm.rotateAngleY += b.bipedBody.rotateAngleY * 2.0F;
            b.bipedRightArm.rotateAngleZ = MathHelper.sin(b.onGround * (float)Math.PI) * -0.4F;
        }
        
        if (p_78087_7_ instanceof EntityPlayer) {
            if (onGround2 > -9990.0F) {
	        	f6 = onGround2;
	            b.bipedBody.rotateAngleY = MathHelper.sin(MathHelper.sqrt_float(f6) * (float)Math.PI * 2.0F) * 0.2F;
	            b.bipedRightArm.rotationPointZ = MathHelper.sin(b.bipedBody.rotateAngleY) * 5.0F;
	            b.bipedRightArm.rotationPointX = -MathHelper.cos(b.bipedBody.rotateAngleY) * 5.0F;
	            b.bipedLeftArm.rotationPointZ = -MathHelper.sin(b.bipedBody.rotateAngleY) * 5.0F;
	            b.bipedLeftArm.rotationPointX = MathHelper.cos(b.bipedBody.rotateAngleY) * 5.0F;
	            b.bipedRightArm.rotateAngleY += b.bipedBody.rotateAngleY;
	            b.bipedLeftArm.rotateAngleY += b.bipedBody.rotateAngleY;
	            b.bipedLeftArm.rotateAngleX += b.bipedBody.rotateAngleY;
	            f6 = 1.0F - onGround2;
	            f6 *= f6;
	            f6 *= f6;
	            f6 = 1.0F - f6;
	            f7 = MathHelper.sin(f6 * (float)Math.PI);
	            float f8 = MathHelper.sin(onGround2 * (float)Math.PI) * -(b.bipedHead.rotateAngleX - 0.7F) * 0.75F;
	            b.bipedLeftArm.rotateAngleX = (float)((double)b.bipedLeftArm.rotateAngleX - ((double)f7 * 1.2D + (double)f8));
	            b.bipedLeftArm.rotateAngleY -= b.bipedBody.rotateAngleY * 2.0F;
				b.bipedLeftArm.rotateAngleZ = MathHelper.sin(onGround2  * (float)Math.PI) * -0.4F;
            }
        }

        if (b.isSneak)
        {
            b.bipedBody.rotateAngleX = 0.5F;
            b.bipedRightArm.rotateAngleX += 0.4F;
            b.bipedLeftArm.rotateAngleX += 0.4F;
            b.bipedRightLeg.rotationPointZ = 4.0F;
            b.bipedLeftLeg.rotationPointZ = 4.0F;
            b.bipedRightLeg.rotationPointY = 9.0F;
            b.bipedLeftLeg.rotationPointY = 9.0F;
            b.bipedHead.rotationPointY = 1.0F;
            b.bipedHeadwear.rotationPointY = 1.0F;
        }
        else
        {
            b.bipedBody.rotateAngleX = 0.0F;
            b.bipedRightLeg.rotationPointZ = 0.1F;
            b.bipedLeftLeg.rotationPointZ = 0.1F;
            b.bipedRightLeg.rotationPointY = 12.0F;
            b.bipedLeftLeg.rotationPointY = 12.0F;
            b.bipedHead.rotationPointY = 0.0F;
            b.bipedHeadwear.rotationPointY = 0.0F;
        }

        b.bipedRightArm.rotateAngleZ += MathHelper.cos(p_78087_3_ * 0.09F) * 0.05F + 0.05F;
        b.bipedLeftArm.rotateAngleZ -= MathHelper.cos(p_78087_3_ * 0.09F) * 0.05F + 0.05F;
        b.bipedRightArm.rotateAngleX += MathHelper.sin(p_78087_3_ * 0.067F) * 0.05F;
        b.bipedLeftArm.rotateAngleX -= MathHelper.sin(p_78087_3_ * 0.067F) * 0.05F;

        if (b.aimedBow)
        {
            f6 = 0.0F;
            f7 = 0.0F;
            b.bipedRightArm.rotateAngleZ = 0.0F;
            b.bipedLeftArm.rotateAngleZ = 0.0F;
            b.bipedRightArm.rotateAngleY = -(0.1F - f6 * 0.6F) + b.bipedHead.rotateAngleY;
            b.bipedLeftArm.rotateAngleY = 0.1F - f6 * 0.6F + b.bipedHead.rotateAngleY + 0.4F;
            b.bipedRightArm.rotateAngleX = -((float)Math.PI / 2F) + b.bipedHead.rotateAngleX;
            b.bipedLeftArm.rotateAngleX = -((float)Math.PI / 2F) + b.bipedHead.rotateAngleX;
            b.bipedRightArm.rotateAngleX -= f6 * 1.2F - f7 * 0.4F;
            b.bipedLeftArm.rotateAngleX -= f6 * 1.2F - f7 * 0.4F;
            b.bipedRightArm.rotateAngleZ += MathHelper.cos(p_78087_3_ * 0.09F) * 0.05F + 0.05F;
            b.bipedLeftArm.rotateAngleZ -= MathHelper.cos(p_78087_3_ * 0.09F) * 0.05F + 0.05F;
            b.bipedRightArm.rotateAngleX += MathHelper.sin(p_78087_3_ * 0.067F) * 0.05F;
            b.bipedLeftArm.rotateAngleX -= MathHelper.sin(p_78087_3_ * 0.067F) * 0.05F;
        }
    }

	@SideOnly(Side.CLIENT)
	public static void customRenderItemInFirstPerson(ItemRenderer iitm, float p_78440_1_)
    {
		renderingItem2 = true;
        float f1 = iitm.prevEquippedProgress + (iitm.equippedProgress - iitm.prevEquippedProgress) * p_78440_1_;
        EntityClientPlayerMP entityclientplayermp = iitm.mc.thePlayer;
        float f2 = entityclientplayermp.prevRotationPitch + (entityclientplayermp.rotationPitch - entityclientplayermp.prevRotationPitch) * p_78440_1_;
        GL11.glPushMatrix();
        GL11.glRotatef(f2, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(entityclientplayermp.prevRotationYaw + (entityclientplayermp.rotationYaw - entityclientplayermp.prevRotationYaw) * p_78440_1_, 0.0F, 1.0F, 0.0F);
        enableStandardShading();
        GL11.glPopMatrix();
        EntityPlayerSP entityplayersp = entityclientplayermp;
        float f3 = entityplayersp.prevRenderArmPitch + (entityplayersp.renderArmPitch - entityplayersp.prevRenderArmPitch) * p_78440_1_;
        float f4 = entityplayersp.prevRenderArmYaw + (entityplayersp.renderArmYaw - entityplayersp.prevRenderArmYaw) * p_78440_1_;
        GL11.glRotatef((entityclientplayermp.rotationPitch - f3) * 0.1F, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef((entityclientplayermp.rotationYaw - f4) * 0.1F, 0.0F, 1.0F, 0.0F);
        boolean lessThan = ((InventoryPlayerBattle)entityclientplayermp.inventory).currentItem <= 153;
        ItemStack itemstack = ((InventoryPlayerBattle)entityclientplayermp.inventory).getStackInSlot(lessThan ? ((InventoryPlayerBattle)entityclientplayermp.inventory).currentItem : ((InventoryPlayerBattle)entityclientplayermp.inventory).currentItem - BattlemodeHookContainerClass.prevOffhandOffset);

        if (itemstack != null && itemstack.getItem() instanceof ItemCloth)
        {
            GL11.glEnable(GL11.GL_BLEND);
            OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        }

        int i = iitm.mc.theWorld.getLightBrightnessForSkyBlocks(MathHelper.floor_double(entityclientplayermp.posX), MathHelper.floor_double(entityclientplayermp.posY), MathHelper.floor_double(entityclientplayermp.posZ), 0);
        int j = i % 65536;
        int k = i / 65536;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, j / 1.0F, k / 1.0F);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        float f5;
        float f6;
        float f7;

        if (itemstack != null)
        {
            int l = itemstack.getItem().getColorFromItemStack(itemstack, 0);
            f5 = (l >> 16 & 255) / 255.0F;
            f6 = (l >> 8 & 255) / 255.0F;
            f7 = (l & 255) / 255.0F;
            GL11.glColor4f(f5, f6, f7, 1.0F);
        }
        else
        {
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        }

        float f8;
        float f9;
        float f10;
        float f13;
        Render render;
        RenderPlayer renderplayer;

        if (itemstack != null && itemstack.getItem() instanceof ItemMap)
        {
            GL11.glPushMatrix();
            f13 = 0.8F;
            f5 = entityclientplayermp.getSwingProgress(p_78440_1_);
            f6 = MathHelper.sin(f5 * (float)Math.PI);
            f7 = MathHelper.sin(MathHelper.sqrt_float(f5) * (float)Math.PI);
            GL11.glTranslatef(-f7 * 0.4F, MathHelper.sin(MathHelper.sqrt_float(f5) * (float)Math.PI * 2.0F) * 0.2F, -f6 * 0.2F);
            f5 = 1.0F - f2 / 45.0F + 0.1F;

            if (f5 < 0.0F)
            {
                f5 = 0.0F;
            }

            if (f5 > 1.0F)
            {
                f5 = 1.0F;
            }

            f5 = -MathHelper.cos(f5 * (float)Math.PI) * 0.5F + 0.5F;
            GL11.glTranslatef(0.0F, 0.0F * f13 - (1.0F - f1) * 1.2F - f5 * 0.5F + 0.04F, -0.9F * f13);
            GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(f5 * -85.0F, 0.0F, 0.0F, 1.0F);
            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
            iitm.mc.getTextureManager().bindTexture(entityclientplayermp.getLocationSkin());

            for (int i1 = 0; i1 < 2; ++i1)
            {
                int j1 = i1 * 2 - 1;
                GL11.glPushMatrix();
                GL11.glTranslatef(-0.0F, -0.6F, 1.1F * j1);
                GL11.glRotatef(-45 * j1, 1.0F, 0.0F, 0.0F);
                GL11.glRotatef(-90.0F, 0.0F, 0.0F, 1.0F);
                GL11.glRotatef(59.0F, 0.0F, 0.0F, 1.0F);
                GL11.glRotatef(-65 * j1, 0.0F, 1.0F, 0.0F);
                render = RenderManager.instance.getEntityRenderObject(iitm.mc.thePlayer);
                renderplayer = (RenderPlayer)render;
                f10 = 1.0F;
                GL11.glScalef(f10, f10, f10);
                renderplayer.renderFirstPersonArm(iitm.mc.thePlayer);
                GL11.glPopMatrix();
            }

            f6 = entityclientplayermp.getSwingProgress(p_78440_1_);
            f7 = MathHelper.sin(f6 * f6 * (float)Math.PI);
            f8 = MathHelper.sin(MathHelper.sqrt_float(f6) * (float)Math.PI);
            GL11.glRotatef(-f7 * 20.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(-f8 * 20.0F, 0.0F, 0.0F, 1.0F);
            GL11.glRotatef(-f8 * 80.0F, 1.0F, 0.0F, 0.0F);
            f9 = 0.38F;
            GL11.glScalef(f9, f9, f9);
            GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);
            GL11.glTranslatef(-1.0F, -1.0F, 0.0F);
            f10 = 0.015625F;
            GL11.glScalef(f10, f10, f10);
            iitm.mc.getTextureManager().bindTexture(ItemRenderer.RES_MAP_BACKGROUND);
            Tessellator tessellator = Tessellator.instance;
            GL11.glNormal3f(0.0F, 0.0F, -1.0F);
            tessellator.startDrawingQuads();
            byte b0 = 7;
            tessellator.addVertexWithUV(0 - b0, 128 + b0, 0.0D, 0.0D, 1.0D);
            tessellator.addVertexWithUV(128 + b0, 128 + b0, 0.0D, 1.0D, 1.0D);
            tessellator.addVertexWithUV(128 + b0, 0 - b0, 0.0D, 1.0D, 0.0D);
            tessellator.addVertexWithUV(0 - b0, 0 - b0, 0.0D, 0.0D, 0.0D);
            tessellator.draw();

            IItemRenderer custom = MinecraftForgeClient.getItemRenderer(itemstack, FIRST_PERSON_MAP);
            MapData mapdata = ((ItemMap)itemstack.getItem()).getMapData(itemstack, iitm.mc.theWorld);

            if (custom == null)
            {
                if (mapdata != null)
                {
                    iitm.mc.entityRenderer.getMapItemRenderer().func_148250_a(mapdata, false);
                }
            }
            else
            {
                custom.renderItem(FIRST_PERSON_MAP, itemstack, iitm.mc.thePlayer, iitm.mc.getTextureManager(), mapdata);
            }

            GL11.glPopMatrix();
        }
        else if (itemstack != null)
        {
            GL11.glPushMatrix();
            f13 = 0.8F;

            if (entityclientplayermp.getItemInUseCount() > 0)
            {
                EnumAction enumaction = ((InventoryPlayerBattle)entityclientplayermp.inventory).getCurrentItem().getItemUseAction();
                if (enumaction != entityclientplayermp.getItemInUse().getItemUseAction()) enumaction = EnumAction.none;
                if (!Minecraft.getMinecraft().gameSettings.keyBindAttack.getIsKeyPressed()) enumaction = EnumAction.none;
                
                if (enumaction == EnumAction.eat || enumaction == EnumAction.drink)
                {
                    f6 = entityclientplayermp.getItemInUseCount() - p_78440_1_ + 1.0F;
                    f7 = 1.0F - f6 / itemstack.getMaxItemUseDuration();
                    f8 = 1.0F - f7;
                    f8 = f8 * f8 * f8;
                    f8 = f8 * f8 * f8;
                    f8 = f8 * f8 * f8;
                    f9 = 1.0F - f8;
                    GL11.glTranslatef(0.0F, MathHelper.abs(MathHelper.cos(f6 / 4.0F * (float)Math.PI) * 0.1F) * (f7 > 0.2D ? 1 : 0), 0.0F);
                    GL11.glTranslatef(f9 * 0.6F, -f9 * 0.5F, 0.0F);
                    GL11.glRotatef(f9 * 90.0F, 0.0F, 1.0F, 0.0F);
                    GL11.glRotatef(f9 * 10.0F, 1.0F, 0.0F, 0.0F);
                    GL11.glRotatef(f9 * 30.0F, 0.0F, 0.0F, 1.0F);
                }
            }
            else
            {
                f5 = entityclientplayermp.getSwingProgress(p_78440_1_);
                f6 = MathHelper.sin(f5 * (float)Math.PI);
                f7 = MathHelper.sin(MathHelper.sqrt_float(f5) * (float)Math.PI);
                GL11.glTranslatef(-f7 * 0.4F, MathHelper.sin(MathHelper.sqrt_float(f5) * (float)Math.PI * 2.0F) * 0.2F, -f6 * 0.2F);
            }

            GL11.glTranslatef(0.7F * f13, -0.65F * f13 - (1.0F - f1) * 0.6F, -0.9F * f13);
            GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
            f5 = entityclientplayermp.getSwingProgress(p_78440_1_);
            f6 = MathHelper.sin(f5 * f5 * (float)Math.PI);
            f7 = MathHelper.sin(MathHelper.sqrt_float(f5) * (float)Math.PI);
            GL11.glRotatef(-f6 * 20.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(-f7 * 20.0F, 0.0F, 0.0F, 1.0F);
            GL11.glRotatef(-f7 * 80.0F, 1.0F, 0.0F, 0.0F);
            f8 = 0.4F;
            GL11.glScalef(f8, f8, f8);
            float f11;
            float f12;

            if (entityclientplayermp.getItemInUseCount() > 0)
            {
            	EnumAction enumaction1 = ((InventoryPlayerBattle)entityclientplayermp.inventory).getCurrentItem().getItemUseAction();
            	if (enumaction1 != entityclientplayermp.getItemInUse().getItemUseAction()) enumaction1 = EnumAction.none;
            	if (!Minecraft.getMinecraft().gameSettings.keyBindAttack.getIsKeyPressed()) enumaction1 = EnumAction.none;
            	
                if (enumaction1 == EnumAction.block)
                {
                    GL11.glTranslatef(-0.5F, 0.2F, 0.0F);
                    GL11.glRotatef(30.0F, 0.0F, 1.0F, 0.0F);
                    GL11.glRotatef(-80.0F, 1.0F, 0.0F, 0.0F);
                    GL11.glRotatef(60.0F, 0.0F, 1.0F, 0.0F);
                }
                else if (enumaction1 == EnumAction.bow)
                {
                    GL11.glRotatef(-18.0F, 0.0F, 0.0F, 1.0F);
                    GL11.glRotatef(-12.0F, 0.0F, 1.0F, 0.0F);
                    GL11.glRotatef(-8.0F, 1.0F, 0.0F, 0.0F);
                    GL11.glTranslatef(-0.9F, 0.2F, 0.0F);
                    f10 = itemstack.getMaxItemUseDuration() - (entityclientplayermp.getItemInUseCount() - p_78440_1_ + 1.0F);
                    f11 = f10 / 20.0F;
                    f11 = (f11 * f11 + f11 * 2.0F) / 3.0F;

                    if (f11 > 1.0F)
                    {
                        f11 = 1.0F;
                    }

                    if (f11 > 0.1F)
                    {
                        GL11.glTranslatef(0.0F, MathHelper.sin((f10 - 0.1F) * 1.3F) * 0.01F * (f11 - 0.1F), 0.0F);
                    }

                    GL11.glTranslatef(0.0F, 0.0F, f11 * 0.1F);
                    GL11.glRotatef(-335.0F, 0.0F, 0.0F, 1.0F);
                    GL11.glRotatef(-50.0F, 0.0F, 1.0F, 0.0F);
                    GL11.glTranslatef(0.0F, 0.5F, 0.0F);
                    f12 = 1.0F + f11 * 0.2F;
                    GL11.glScalef(1.0F, 1.0F, f12);
                    GL11.glTranslatef(0.0F, -0.5F, 0.0F);
                    GL11.glRotatef(50.0F, 0.0F, 1.0F, 0.0F);
                    GL11.glRotatef(335.0F, 0.0F, 0.0F, 1.0F);
                }
            }

            if (itemstack.getItem().shouldRotateAroundWhenRendering())
            {
                GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
            }

            if (itemstack.getItem().requiresMultipleRenderPasses())
            {
                iitm.renderItem(entityclientplayermp, itemstack, 0, EQUIPPED_FIRST_PERSON);
                for (int x = 1; x < itemstack.getItem().getRenderPasses(itemstack.getItemDamage()); x++)
                {
                    int k1 = itemstack.getItem().getColorFromItemStack(itemstack, x);
                    f10 = (k1 >> 16 & 255) / 255.0F;
                    f11 = (k1 >> 8 & 255) / 255.0F;
                    f12 = (k1 & 255) / 255.0F;
                    GL11.glColor4f(1.0F * f10, 1.0F * f11, 1.0F * f12, 1.0F);
                    iitm.renderItem(entityclientplayermp, itemstack, x, EQUIPPED_FIRST_PERSON);
                }
            }
            else
            {
                iitm.renderItem(entityclientplayermp, itemstack, 0, EQUIPPED_FIRST_PERSON);
            }

            GL11.glPopMatrix();
        }
        else if (!entityclientplayermp.isInvisible())
        {
            GL11.glPushMatrix();
            f13 = 0.8F;
            f5 = entityclientplayermp.getSwingProgress(p_78440_1_);
            f6 = MathHelper.sin(f5 * (float)Math.PI);
            f7 = MathHelper.sin(MathHelper.sqrt_float(f5) * (float)Math.PI);
            GL11.glTranslatef(-f7 * 0.3F, MathHelper.sin(MathHelper.sqrt_float(f5) * (float)Math.PI * 2.0F) * 0.4F, -f6 * 0.4F);
            GL11.glTranslatef(0.8F * f13, -0.75F * f13 - (1.0F - f1) * 0.6F, -0.9F * f13);
            GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
            f5 = entityclientplayermp.getSwingProgress(p_78440_1_);
            f6 = MathHelper.sin(f5 * f5 * (float)Math.PI);
            f7 = MathHelper.sin(MathHelper.sqrt_float(f5) * (float)Math.PI);
            GL11.glRotatef(f7 * 70.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(-f6 * 20.0F, 0.0F, 0.0F, 1.0F);
            iitm.mc.getTextureManager().bindTexture(entityclientplayermp.getLocationSkin());
            GL11.glTranslatef(-1.0F, 3.6F, 3.5F);
            GL11.glRotatef(120.0F, 0.0F, 0.0F, 1.0F);
            GL11.glRotatef(200.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(-135.0F, 0.0F, 1.0F, 0.0F);
            GL11.glScalef(1.0F, 1.0F, 1.0F);
            GL11.glTranslatef(5.6F, 0.0F, 0.0F);
            render = RenderManager.instance.getEntityRenderObject(iitm.mc.thePlayer);
            renderplayer = (RenderPlayer)render;
            f10 = 1.0F;
            GL11.glScalef(f10, f10, f10);
            renderplayer.renderFirstPersonArm(iitm.mc.thePlayer);
            GL11.glPopMatrix();
        }

        if (itemstack != null && itemstack.getItem() instanceof ItemCloth)
        {
            GL11.glDisable(GL11.GL_BLEND);
        }

        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        RenderHelper.disableStandardItemLighting();
        renderingItem2 = false;
    }
	
	@SideOnly(Side.CLIENT)
	@Fix(insertOnExit = true, returnSetting = EnumReturnSetting.ALWAYS)
    public static IIcon getItemIcon(EntityPlayer p, ItemStack p_70620_1_, int p_70620_2_, @ReturnedValue IIcon returnValue) 
	{
		if (returnValue == Items.bow.getItemIconForUseDuration(2) || returnValue == Items.bow.getItemIconForUseDuration(1) || returnValue == Items.bow.getItemIconForUseDuration(0)) {
			if (p.getItemInUse().getItem() != Items.bow) {
				return p_70620_1_.getItem().getIcon(p_70620_1_, p_70620_2_, p, p.getItemInUse(), p.getItemInUseCount());
			}
		}
		return returnValue;
	}
	
	@Fix(returnSetting=EnumReturnSetting.ON_TRUE)
	public static boolean processPlayerDigging(NetHandlerPlayServer serv, C07PacketPlayerDigging p_147345_1_)
    {
		WorldServer worldserver = MinecraftServer.getServer().worldServerForDimension(serv.playerEntity.dimension);
	    serv.playerEntity.func_143004_u();
	
	    if (p_147345_1_.func_149506_g() == 4)
	    {
	        serv.playerEntity.dropOneItem(false);
	        return true;
	    }
	    else if (p_147345_1_.func_149506_g() == 3)
	    {
	        serv.playerEntity.dropOneItem(true);
	        return true;
	    }
	    else if (p_147345_1_.func_149506_g() == 5)
	    {
	        serv.playerEntity.stopUsingItem();
	        return true;
	    }
	    else
	    {
	        boolean flag = false;
	
	        if (p_147345_1_.func_149506_g() == 0)
	        {
	            flag = true;
	        }
	
	        if (p_147345_1_.func_149506_g() == 1)
	        {
	            flag = true;
	        }
	
	        if (p_147345_1_.func_149506_g() == 2)
	        {
	            flag = true;
	        }
	
	        int i = p_147345_1_.func_149505_c();
	        int j = p_147345_1_.func_149503_d();
	        int k = p_147345_1_.func_149502_e();
	        if (flag)
	        {
	            double d0 = serv.playerEntity.posX - ((double)i + 0.5D);
	            double d1 = serv.playerEntity.posY - ((double)j + 0.5D) + 1.5D;
	            double d2 = serv.playerEntity.posZ - ((double)k + 0.5D);
	            double d3 = d0 * d0 + d1 * d1 + d2 * d2;
	
	            double dist = serv.playerEntity.theItemInWorldManager.getBlockReachDistance() + 1;
	            dist *= dist;
	
	            if (d3 > dist)
	            {
	                return true;
	            }
	        }
	        
	        if (p_147345_1_.func_149506_g() == 2)
	        {
	        	customUncheckedTryHarvestBlock(serv.playerEntity.theItemInWorldManager, i, j, k);
	            serv.playerEntity.theItemInWorldManager.uncheckedTryHarvestBlock(i, j, k);
	
	            if (worldserver.getBlock(i, j, k).getMaterial() != Material.air)
	            {
	                serv.playerEntity.playerNetServerHandler.sendPacket(new S23PacketBlockChange(i, j, k, worldserver));
	            }
	            return true;
	        }
	        else if (p_147345_1_.func_149506_g() == 1)
	        {
	            serv.playerEntity.theItemInWorldManager.cancelDestroyingBlock(i, j, k);
	
	            if (worldserver.getBlock(i, j, k).getMaterial() != Material.air)
	            {
	                serv.playerEntity.playerNetServerHandler.sendPacket(new S23PacketBlockChange(i, j, k, worldserver));
	            }
	            return true;
	        }
	    }
	    return false;
    }
	
	// This might be a bad idea. (but if I didn't do this I would have to insert ~10 more fixes into forge-hooked methods and it might not even have worked)
	@Fix
	public static void uncheckedTryHarvestBlock(ItemInWorldManager m, int p_73082_1_, int p_73082_2_, int p_73082_3_)
    {
        m.theWorld.destroyBlockInWorldPartially(m.thisPlayerMP.getEntityId(), p_73082_1_, p_73082_2_, p_73082_3_, -1);
        m.tryHarvestBlock(p_73082_1_, p_73082_2_, p_73082_3_);
    }
	
	public static void customUncheckedTryHarvestBlock(ItemInWorldManager m, int p_73082_1_, int p_73082_2_, int p_73082_3_)
    {
        m.theWorld.destroyBlockInWorldPartially(m.thisPlayerMP.getEntityId(), p_73082_1_, p_73082_2_, p_73082_3_, -1);
        m.tryHarvestBlock(p_73082_1_, p_73082_2_, p_73082_3_);
    }

	

	@Fix(returnSetting = EnumReturnSetting.ALWAYS)
	public static void attackTargetEntityWithCurrentItem(EntityPlayer plr, Entity p_71059_1_)
    {
        if (MinecraftForge.EVENT_BUS.post(new AttackEntityEvent(plr, p_71059_1_)))
        {
            return;
        }
        ItemStack stack = plr.getCurrentEquippedItem();
        if (stack != null && stack.getItem().onLeftClickEntity(stack, plr, p_71059_1_))
        {
            return;
        }
        if (p_71059_1_.canAttackWithItem())
        {
            if (!p_71059_1_.hitByEntity(plr))
            {
                float f = (float)plr.getEntityAttribute(SharedMonsterAttributes.attackDamage).getAttributeValue();
                int i = 0;
                float f1 = 0.0F;

                if (p_71059_1_ instanceof EntityLivingBase)
                {
                    f1 = EnchantmentHelper.getEnchantmentModifierLiving(plr, (EntityLivingBase)p_71059_1_);
                    i += EnchantmentHelper.getKnockbackModifier(plr, (EntityLivingBase)p_71059_1_);
                }

                if (plr.isSprinting())
                {
                    ++i;
                }

                if (f > 0.0F || f1 > 0.0F)
                {
                    boolean flag = plr.fallDistance > 0.0F && !plr.onGround && !plr.isOnLadder() && !plr.isInWater() && !plr.isPotionActive(Potion.blindness) && plr.ridingEntity == null && p_71059_1_ instanceof EntityLivingBase;

                    if (flag && f > 0.0F)
                    {
                        f *= 1.5F;
                    }

                    ItemStack offItem = ((InventoryPlayerBattle)plr.inventory).getCurrentOffhandWeapon();

                    if(offItem != null && offItem.getItem() instanceof ItemSword) {
                    	f += ToolMaterial.valueOf(((ItemSword)offItem.getItem()).getToolMaterialName()).getDamageVsEntity() + 3F;
                    } else if (offItem != null && offItem.getItem() instanceof ItemTool) {
                    	f += ToolMaterial.valueOf(((ItemTool)offItem.getItem()).getToolMaterialName()).getDamageVsEntity() + 1F;
                    }

                    f += f1;
                    boolean flag1 = false;
                    int j = EnchantmentHelper.getFireAspectModifier(plr);

                    if (p_71059_1_ instanceof EntityLivingBase && j > 0 && !p_71059_1_.isBurning())
                    {
                        flag1 = true;
                        p_71059_1_.setFire(1);
                    }

                    boolean flag2 = p_71059_1_.attackEntityFrom(DamageSource.causePlayerDamage(plr), f);

                    if (flag2)
                    {
                        if (i > 0)
                        {
                            p_71059_1_.addVelocity(-MathHelper.sin(plr.rotationYaw * (float)Math.PI / 180.0F) * i * 0.5F, 0.1D, MathHelper.cos(plr.rotationYaw * (float)Math.PI / 180.0F) * i * 0.5F);
                            plr.motionX *= 0.6D;
                            plr.motionZ *= 0.6D;
                            plr.setSprinting(false);
                        }

                        if (flag)
                        {
                            plr.onCriticalHit(p_71059_1_);
                        }

                        if (f1 > 0.0F)
                        {
                            plr.onEnchantmentCritical(p_71059_1_);
                        }

                        if (f >= 18.0F)
                        {
                            plr.triggerAchievement(AchievementList.overkill);
                        }

                        plr.setLastAttacker(p_71059_1_);

                        if (p_71059_1_ instanceof EntityLivingBase)
                        {
                            EnchantmentHelper.func_151384_a((EntityLivingBase)p_71059_1_, plr);
                        }

                        EnchantmentHelper.func_151385_b(plr, p_71059_1_);
                        ItemStack itemstack = plr.getCurrentEquippedItem();
                        Object object = p_71059_1_;

                        if (p_71059_1_ instanceof EntityDragonPart)
                        {
                            IEntityMultiPart ientitymultipart = ((EntityDragonPart)p_71059_1_).entityDragonObj;

                            if (ientitymultipart != null && ientitymultipart instanceof EntityLivingBase)
                            {
                                object = ientitymultipart;
                            }
                        }

                        if (itemstack != null && object instanceof EntityLivingBase)
                        {
                            itemstack.hitEntity((EntityLivingBase)object, plr);

                            if (itemstack.stackSize <= 0)
                            {
                                plr.destroyCurrentEquippedItem();
                            }
                        }

                        if (p_71059_1_ instanceof EntityLivingBase)
                        {
                            plr.addStat(StatList.damageDealtStat, Math.round(f * 10.0F));

                            if (j > 0)
                            {
                                p_71059_1_.setFire(j * 4);
                            }
                        }

                        plr.addExhaustion(0.3F);
                    }
                    else if (flag1)
                    {
                        p_71059_1_.extinguish();
                    }
                }
            }
        }
    }

	@Fix(returnSetting=EnumReturnSetting.ON_TRUE)
	public static boolean onPlayerStoppedUsing(ItemBow bow, ItemStack p_77615_1_, World p_77615_2_, EntityPlayer p_77615_3_, int p_77615_4_)
    {
        if (!doubleBow) return false;
		if (((InventoryPlayerBattle)p_77615_3_.inventory).getCurrentOffhandWeapon() != null && ((InventoryPlayerBattle)p_77615_3_.inventory).getCurrentItem() != null && ((InventoryPlayerBattle)p_77615_3_.inventory).getCurrentOffhandWeapon().getItem() == Items.bow && ((InventoryPlayerBattle)p_77615_3_.inventory).getCurrentItem().getItem() == Items.bow) {
        } else {
        	return false;
        }

        int j = bow.getMaxItemUseDuration(p_77615_1_) - p_77615_4_;

        ArrowLooseEvent event = new ArrowLooseEvent(p_77615_3_, p_77615_1_, j);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isCanceled())
        {
            return true;
        }
        j = event.charge;

        boolean flag = p_77615_3_.capabilities.isCreativeMode || EnchantmentHelper.getEnchantmentLevel(Enchantment.infinity.effectId, p_77615_1_) > 0;

        if (flag || p_77615_3_.inventory.hasItem(Items.arrow))
        {
            float f = j / 20.0F;
            f = (f * f + f * 2.0F) / 3.0F;

            if (f < 0.1D)
            {
                return true;
            }

            if (f > 1.0F)
            {
                f = 1.0F;
            }

            EntityArrow entityarrow = new EntityArrow(p_77615_2_, p_77615_3_, f * 2.0F);
            EntityArrow entityarrow2 = new EntityArrow(p_77615_2_, p_77615_3_, f * 1.5F);

            if (flag)
            {
                entityarrow.canBePickedUp = 2;
                entityarrow2.canBePickedUp = 2;
            }
            else
            {
                p_77615_3_.inventory.consumeInventoryItem(Items.arrow);
            }

            boolean hasEnough = p_77615_3_.inventory.hasItem(Items.arrow) || flag;

            if (!hasEnough) {
            	p_77615_3_.inventory.addItemStackToInventory(new ItemStack(Items.arrow, 1));
            	return true;
            } else {
            	if (!flag) p_77615_3_.inventory.consumeInventoryItem(Items.arrow);
            }

            if (f == 1.0F)
            {
                entityarrow.setIsCritical(true);
                entityarrow2.setIsCritical(true);
            }

            int k = EnchantmentHelper.getEnchantmentLevel(Enchantment.power.effectId, p_77615_1_);

            if (k > 0)
            {
                entityarrow.setDamage(entityarrow.getDamage() + k * 0.5D + 0.5D);
                entityarrow2.setDamage(entityarrow2.getDamage() + k * 0.5D + 0.5D);
            }

            int l = EnchantmentHelper.getEnchantmentLevel(Enchantment.punch.effectId, p_77615_1_);

            if (l > 0)
            {
                entityarrow.setKnockbackStrength(l);
                entityarrow2.setKnockbackStrength(l);
            }

            if (EnchantmentHelper.getEnchantmentLevel(Enchantment.flame.effectId, p_77615_1_) > 0)
            {
                entityarrow.setFire(100);
                entityarrow2.setFire(100);
            }

            p_77615_1_.damageItem(1, p_77615_3_);
            p_77615_2_.playSoundAtEntity(p_77615_3_, "random.bow", 1.0F, 1.0F / (p_77615_2_.rand.nextFloat() * 0.4F + 1.2F) + f * 0.5F);

            if (!p_77615_2_.isRemote)
            {
                p_77615_2_.spawnEntityInWorld(entityarrow);
                p_77615_2_.spawnEntityInWorld(entityarrow2);

            }
        }

        if (((InventoryPlayerBattle)p_77615_3_.inventory).getCurrentOffhandWeapon() != null && ((InventoryPlayerBattle)p_77615_3_.inventory).getCurrentItem() != null && ((InventoryPlayerBattle)p_77615_3_.inventory).getCurrentOffhandWeapon().getItem() instanceof ItemBow && ((InventoryPlayerBattle)p_77615_3_.inventory).getCurrentItem().getItem() instanceof ItemBow) {
        	return true;
        } else {
        	return false;
        }
    }

    private static final MethodHandle fieldSet;
    private static final Field field;

    private static final MethodHandle fieldGetSection;
    private static final MethodHandle fieldGetContainerMgr;

//    private static final MethodHandle fieldSetLightLevel;
//    private static final Field fieldlightlevel;
//
//    private static final MethodHandle fieldGetEntity;
//    private static final Field fieldentity;
//
//    private static final MethodHandle fieldGetEnabled;
//    private static final Field fieldenabled;

    static {
        MethodHandle fs, fs2, fg, fg2, fg3;
        Field f, f2, f3, f4;
        try {
            f = HandlerCheckPin.class.getDeclaredField("shouldReset");
            f.setAccessible(true);
            fs = MethodHandles.publicLookup().unreflectSetter(f);

            f2 = InvTweaksContainerSectionManager.class.getDeclaredField("containerMgr");
            f3 = InvTweaksContainerSectionManager.class.getDeclaredField("section");

            f2.setAccessible(true);
            f3.setAccessible(true);

            fg = MethodHandles.publicLookup().unreflectGetter(f2);
            fg2 = MethodHandles.publicLookup().unreflectGetter(f3);
        } catch (Exception e) {
            f = null;
            fs = null;
            fg = null;
            fg2 = null;
            System.out.println("The 'Locks' mod compatibility hasn't been loaded due to not being able to find HandlerCheckPin. " +
                    "If you don't have the Locks mod installed, you can ignore this error.");
        } catch (NoClassDefFoundError e) {
            f = null;
            fs = null;
            fg = null;
            fg2 = null;
            System.out.println("The 'Locks' mod compatibility hasn't been loaded due to not being able to find HandlerCheckPin. " +
                    "If you don't have the Locks mod installed, you can ignore this error.");
        }

//        try {
//            f2 = PlayerSelfAdaptor.class.getDeclaredField("thePlayer");
//            f3 = BaseAdaptor.class.getDeclaredField("lightLevel");
//            f4 = BaseAdaptor.class.getDeclaredField("enabled");
//
//            f2.setAccessible(true);
//            f3.setAccessible(true);
//            f4.setAccessible(true);
//
//            fs2 = MethodHandles.publicLookup().unreflectSetter(f3);
//            fg = MethodHandles.publicLookup().unreflectGetter(f2);
//            fg2 = MethodHandles.publicLookup().unreflectGetter(f4);
//        } catch (Exception e) {
//            f2 = f3 = f4 = null;
//            fs2 = fg = fg2 = fg3 = null;
//            System.out.println("The 'Dynamic Lights' mod compatibility hasn't been loaded due to not being able to find BaseAdaptor. " +
//                    "If you don't have the Dynamic Lights mod installed, you can ignore this error.");
//        } catch (NoClassDefFoundError e) {
//            f2 = f3 = f4 = null;
//            fs2 = fg = fg2 = fg3 = null;
//            System.out.println("The 'Dynamic Lights' mod compatibility hasn't been loaded due to not being able to find BaseAdaptor. " +
//                    "If you don't have the Dynamic Lights mod installed, you can ignore this error.");
//        }

        field = f;
        fieldSet = fs;

        fieldGetContainerMgr = fg;
        fieldGetSection = fg2;

//        fieldlightlevel = f3;
//        fieldentity = f2;
//        fieldenabled = f4;
//
//        fieldGetEnabled = fg2;
//        fieldGetEntity = fg;
//        fieldSetLightLevel = fs2;

        System.out.println("Loaded Mod Compatibility!");
    }

    //InvTweaksContainerManager containerMgr;
    //ContainerSection section;

    @Optional.Method(modid="inventorytweaks")
    public static ContainerSection getContainerSection(InvTweaksContainerSectionManager itcm) {
        ContainerSection section;
        try {
            section = (ContainerSection) fieldGetSection.invokeExact((InvTweaksContainerSectionManager)itcm);
        } catch (Throwable e) {
            System.out.println("The 'Inventory Tweaks' mod compatibility hasn't been loaded due to not being able to find ContainerSection. " +
                    "If you don't have the 'Inventory Tweaks' mod installed, you can ignore this error.");
            section = null;
        }
        return section;
    }

    @Optional.Method(modid="inventorytweaks")
    public static InvTweaksContainerManager getContainerManager(InvTweaksContainerSectionManager itcm) {
        InvTweaksContainerManager manager;
        try {
            manager = (InvTweaksContainerManager) fieldGetContainerMgr.invokeExact((InvTweaksContainerSectionManager)itcm);
        } catch (Throwable e) {
            System.out.println("The 'Inventory Tweaks' mod compatibility hasn't been loaded due to not being able to find InvTweaksContainerManager. " +
                    "If you don't have the 'Inventory Tweaks' mod installed, you can ignore this error.");
            manager = null;
        }
        return manager;
    }

    // inv tweaks compat starts here

    @Optional.Method(modid="inventorytweaks")
    @Fix(returnSetting = EnumReturnSetting.ALWAYS)
    public static boolean move(InvTweaksContainerSectionManager itcm, int srcIndex, int destIndex) {
        if (srcIndex >= 150) {
            srcIndex -= 150;
            consecutiveCancel = true;
            return false;
        }

        if (consecutiveCancel) {
            consecutiveCancel = false;
            return false;
        }

        if (destIndex >= 150) destIndex -= 150;

        return getContainerManager(itcm).move(getContainerSection(itcm), srcIndex, getContainerSection(itcm), destIndex);
    }

    @Optional.Method(modid="inventorytweaks")
    @Fix(returnSetting = EnumReturnSetting.ALWAYS)
    public static boolean moveSome(InvTweaksContainerSectionManager itcm, int srcIndex, int destIndex, int amount) {
        if (srcIndex >= 150) srcIndex -= 150;
        if (destIndex >= 150) destIndex -= 150;
        return getContainerManager(itcm).moveSome(getContainerSection(itcm), srcIndex, getContainerSection(itcm), destIndex, amount);
    }

    @Optional.Method(modid="inventorytweaks")
    @Fix(returnSetting = EnumReturnSetting.ALWAYS)
    public static boolean drop(InvTweaksContainerSectionManager itcm, int srcIndex) {
        if (srcIndex >= 150) srcIndex -= 150;
        return getContainerManager(itcm).drop(getContainerSection(itcm), srcIndex);
    }

    @Optional.Method(modid="inventorytweaks")
    @Fix(returnSetting = EnumReturnSetting.ALWAYS)
    public static boolean dropSome(InvTweaksContainerSectionManager itcm, int srcIndex, int amount) throws TimeoutException {
        if (srcIndex >= 150) srcIndex -= 150;
        return getContainerManager(itcm).dropSome(getContainerSection(itcm), srcIndex, amount);
    }

    @Optional.Method(modid="inventorytweaks")
    @Fix(returnSetting = EnumReturnSetting.ALWAYS)
    public static boolean putHoldItemDown(InvTweaksContainerSectionManager itcm, int destIndex) throws TimeoutException {
        if (destIndex >= 150) destIndex -= 150;
        return getContainerManager(itcm).putHoldItemDown(getContainerSection(itcm), destIndex);
    }

    @Optional.Method(modid="inventorytweaks")
    @Fix(returnSetting = EnumReturnSetting.ALWAYS)
    public static void leftClick(InvTweaksContainerSectionManager itcm, int index) throws TimeoutException {
        if (index >= 150) index -= 150;
        getContainerManager(itcm).leftClick(getContainerSection(itcm), index);
    }

    @Optional.Method(modid="inventorytweaks")
    @Fix(returnSetting = EnumReturnSetting.ALWAYS)
    public static void rightClick(InvTweaksContainerSectionManager itcm, int index) throws TimeoutException {
        if (index >= 150) index -= 150;
        getContainerManager(itcm).rightClick(getContainerSection(itcm), index);
    }

    @Optional.Method(modid="inventorytweaks")
    @Fix(returnSetting = EnumReturnSetting.ALWAYS)
    public static void click(InvTweaksContainerSectionManager itcm, int index, boolean rightClick) throws TimeoutException {
        if (index >= 150) index -= 150;
        getContainerManager(itcm).click(getContainerSection(itcm), index, rightClick);
    }

    @Optional.Method(modid="inventorytweaks")
    @Fix(returnSetting = EnumReturnSetting.ALWAYS)
    public static boolean isSlotEmpty(InvTweaksContainerSectionManager itcm, int slot) {
        if (slot >= 150) slot -= 150;
        return getContainerManager(itcm).isSlotEmpty(getContainerSection(itcm), slot);
    }

    @Optional.Method(modid="inventorytweaks")
    @Fix(returnSetting = EnumReturnSetting.ALWAYS)
    public static Slot getSlot(InvTweaksContainerSectionManager itcm, int index) {
        if (index >= 150) index -= 150;
        return getContainerManager(itcm).getSlot(getContainerSection(itcm), index);
    }

    @Optional.Method(modid="inventorytweaks")
    @Fix(returnSetting = EnumReturnSetting.ALWAYS)
    public static int getSlotIndex(InvTweaksContainerSectionManager itcm, int slotNumber) {
        if (slotNumber >= 150) slotNumber -= 150;
        return itcm.isSlotInSection(slotNumber) ? getContainerManager(itcm).getSlotIndex(slotNumber) : -1;
    }

    @Optional.Method(modid="inventorytweaks")
    @Fix(returnSetting = EnumReturnSetting.ALWAYS)
    public static boolean isSlotInSection(InvTweaksContainerSectionManager itcm, int slotNumber) {
        if (slotNumber >= 150) slotNumber -= 150;
        return getContainerManager(itcm).getSlotSection(slotNumber) == getContainerSection(itcm);
    }

    @Optional.Method(modid="inventorytweaks")
    @Fix(returnSetting = EnumReturnSetting.ALWAYS)
    public static ItemStack getItemStack(InvTweaksContainerSectionManager itcm, int index) throws NullPointerException, IndexOutOfBoundsException {
        if (index >= 150) index -= 150;
        return getContainerManager(itcm).getItemStack(getContainerSection(itcm), index);
    }

    private static int indexToSlot(InvTweaksContainerManager itcm, ContainerSection section, int index) {
        if (index == -999) {
            return -999;
        } else if (itcm.hasSection(section)) {
            Slot slot = (Slot)(itcm.getSlots(section)).get(index);
            return slot != null ? InvTweaksObfuscation.getSlotNumber(slot) : -1;
        } else {
            return -1;
        }
    }

    // offhand mod compatibility for dynamic lights

    // doesn't work??!? Why?!?

//    @Optional.Method(modid="DynamicLights")
//    @Fix(returnSetting = EnumReturnSetting.ALWAYS)
//    public static void onTick(PlayerSelfAdaptor psa) {
//        EntityPlayer thePlayer = null;
//        boolean enabled = false;
//        try {
//            thePlayer = (EntityPlayer) fieldGetEntity.invokeExact((PlayerSelfAdaptor)psa);
//            enabled = (boolean) fieldGetEnabled.invokeExact((BaseAdaptor)psa);
//        } catch (Throwable e) {
//            System.out.println("The 'Dynamic Lights' mod compatibility hasn't been loaded due to not being able to find thePlayer/enabled. " +
//                    "If you don't have the Dynamic Lights mod installed, you can ignore this error.");
//        }
//        try {
//            if (thePlayer != null && thePlayer.isEntityAlive() && !DynamicLights.globalLightsOff) {
//                List messages = FMLInterModComms.fetchRuntimeMessages(psa);
//                if (messages.size() > 0) {
//                    FMLInterModComms.IMCMessage imcMessage = (FMLInterModComms.IMCMessage) messages.get(messages.size() - 1);
//                    if (imcMessage.key.equalsIgnoreCase("forceplayerlighton")) {
//                        if (!DynamicLights.fmlOverrideEnable) {
//                            DynamicLights.fmlOverrideEnable = true;
//                            if (!enabled) {
//                                fieldSetLightLevel.invokeExact((BaseAdaptor) psa, 15);
//                                psa.enableLight();
//                            }
//                        }
//                    } else if (imcMessage.key.equalsIgnoreCase("forceplayerlightoff") && DynamicLights.fmlOverrideEnable) {
//                        DynamicLights.fmlOverrideEnable = false;
//                        if (enabled) {
//                            psa.disableLight();
//                        }
//                    }
//                }
//
//                if (!DynamicLights.fmlOverrideEnable) {
//                    int prevLight = psa.getLightLevel();
//
//                    ItemStack mainhanditem = thePlayer.getCurrentEquippedItem();
//                    ItemStack offhandItem = ((InventoryPlayerBattle) thePlayer.inventory).getCurrentOffhandWeapon();
//                    int lightlevel1 = Config.itemsMap.getLightFromItemStack(mainhanditem);
//                    int lightlevel2 = Config.itemsMap.getLightFromItemStack(offhandItem);
//
//                    fieldSetLightLevel.invokeExact((BaseAdaptor) psa, lightlevel1 > lightlevel2 ? lightlevel1 : lightlevel2);
//                    ItemStack item = lightlevel1 > lightlevel2 ? mainhanditem : offhandItem;
//
//                    ItemStack[] var4 = thePlayer.inventory.armorInventory;
//                    int var5 = var4.length;
//
//                    int var6;
//                    ItemStack armor;
//                    for (var6 = 0; var6 < var5; ++var6) {
//                        armor = var4[var6];
//                        fieldSetLightLevel.invokeExact((BaseAdaptor) psa, DynamicLights.maxLight(psa.getLightLevel(), Config.itemsMap.getLightFromItemStack(armor)));
//                    }
//
//                    if (prevLight != 0 &&  psa.getLightLevel() != prevLight) {
//                        fieldSetLightLevel.invokeExact((BaseAdaptor) psa, 0);
//                    } else if (thePlayer.isBurning()) {
//                        fieldSetLightLevel.invokeExact((BaseAdaptor) psa, 15);
//                    } else if (checkPlayerWater(thePlayer, psa) && item != null && Config.notWaterProofItems.retrieveValue(GameData.getItemRegistry().getNameForObject(item.getItem()), item.getItemDamage()) == 1) {
//                        fieldSetLightLevel.invokeExact((BaseAdaptor) psa, 0);
//                        var4 = thePlayer.inventory.armorInventory;
//                        var5 = var4.length;
//
//                        for (var6 = 0; var6 < var5; ++var6) {
//                            armor = var4[var6];
//                            if (armor != null && Config.notWaterProofItems.retrieveValue(GameData.getItemRegistry().getNameForObject(armor.getItem()), item.getItemDamage()) == 0) {
//                                fieldSetLightLevel.invokeExact((BaseAdaptor) psa, DynamicLights.maxLight(psa.getLightLevel(), Config.itemsMap.getLightFromItemStack(armor)));
//                            }
//                        }
//                    }
//
//                    checkForchange(psa);
//                }
//            }
//        } catch (Throwable e) {
//            System.out.println("Failed to set dynlights lightLevel correctly.");
//        }
//    }
//
//    @Optional.Method(modid="DynamicLights")
//    private static boolean checkPlayerWater(EntityPlayer thePlayer, PlayerSelfAdaptor psa) {
//        if (thePlayer.isInWater()) {
//            int x = MathHelper.floor_double(thePlayer.posX + 0.5D);
//            int y = MathHelper.floor_double(thePlayer.posY + (double)thePlayer.getEyeHeight());
//            int z = MathHelper.floor_double(thePlayer.posZ + 0.5D);
//            return thePlayer.worldObj.getBlock(x, y, z).getMaterial() == Material.water;
//        } else {
//            return false;
//        }
//    }
//
//    @Optional.Method(modid="DynamicLights")
//    protected static void checkForchange(PlayerSelfAdaptor psa) {
//        boolean enabled = false;
//        try {
//            enabled = (boolean) fieldGetEnabled.invokeExact((BaseAdaptor)psa);
//        } catch (Throwable e) {
//            System.out.println("The 'Dynamic Lights' mod compatibility hasn't been loaded due to not being able to find enabled. " +
//                    "If you don't have the Dynamic Lights mod installed, you can ignore this error.");
//        }
//        if (!enabled && psa.getLightLevel() > 0) {
//            psa.enableLight();
//        } else if (enabled && psa.getLightLevel() < 1) {
//            psa.disableLight();
//        }
//
//    }

    // offhand mod compatibility for Locks

    @Optional.Method(modid="locks")
    private static boolean breakChance(ItemStack itemStack) {
        if (itemStack.getItem() == Locks.itemLockpick) {
            double chance1 = Math.random();
            return chance1 >= 0.7D;
        } else {
            return false;
        }
    }

    @Optional.Method(modid="locks")
    @Fix(returnSetting = EnumReturnSetting.ALWAYS)
    public static boolean breakLockpick(HandlerCheckPin hcp, EntityPlayer player, TileEntityLockableBase tileEntity) {
        try {
            if (player.getHeldItem() != null) {
                if (breakChance(player.getHeldItem())) {
                    if (player.getHeldItem().stackSize <= 1) {
                        player.worldObj.playSoundAtEntity(player, "random.break", 0.8F, 0.85F + player.worldObj.rand.nextFloat() * 0.2F);

                        player.inventory.mainInventory[player.inventory.currentItem >= 150 ? player.inventory.currentItem - 150 + 4 : player.inventory.currentItem + 4] = null;
                        ((InventoryPlayerBattle)player.inventory).hasChanged = true;
                        TheOffhandMod.packetHandler.sendPacketToPlayer(new BattlegearSyncItemPacket(player).generatePacket(), (EntityPlayerMP) player);
                        tileEntity.generatePattern(player.worldObj);
                        fieldSet.invokeExact(hcp, true);
                        return true;
                    } else {
                        player.worldObj.playSoundAtEntity(player, "random.break", 0.8F, 0.85F + player.worldObj.rand.nextFloat() * 0.2F);
                        player.inventory.decrStackSize(player.inventory.currentItem, 1);
                        tileEntity.generatePattern(player.worldObj);
                        fieldSet.invokeExact(hcp, true);
                        return false;
                    }
                } else {
                    fieldSet.invokeExact(hcp, false);
                    return false;
                }
            } else {
                fieldSet.invokeExact(hcp, true);
                return true;
            }
        } catch (Throwable e) {
            throw new RuntimeException("Failed reflection for lockpicks!", e);
        }
    }
// EXAMPLE: (more examples in the official wiki)

      /**
       * Target: every time the window is resized, print the new size
       */
//      @Fix
//      @SideOnly(Side.CLIENT)
//      public static void resize(Minecraft mc, int x, int y) {
//          System.out.println("Resize, x=" + x + ", y=" + y);
//     }

}
