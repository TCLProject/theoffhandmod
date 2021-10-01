package net.tclproject.mysteriumlib.asm.fixes;

import static net.minecraftforge.client.IItemRenderer.ItemRenderType.EQUIPPED_FIRST_PERSON;
import static net.minecraftforge.client.IItemRenderer.ItemRenderType.FIRST_PERSON_MAP;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mods.battlegear2.BattlemodeHookContainerClass;
import mods.battlegear2.api.core.BattlegearUtils;
import mods.battlegear2.api.core.InventoryPlayerBattle;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
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
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Items;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.Item.ToolMaterial;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemCloth;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.potion.Potion;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ItemInWorldManager;
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
import net.tclproject.theoffhandmod.misc.OffhandEventHandler;
import proxy.client.TOMClientProxy;

public class MysteriumPatchesFixesO {

	/**Whether we should override the default action of the item in hand*/
	public static boolean shouldNotOverride;
	/**Whether we have just overriden the minecraft method that gets called on right click to substitute the offhand item*/
	public static boolean leftclicked;

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
        		int slot = ((InventoryPlayerBattle)p.inventory).currentItem - InventoryPlayerBattle.OFFSET;
        		ItemStack item = ((InventoryPlayerBattle)p.inventory).decrStackSize(slot, offCount);
        		return p.func_146097_a(item, false, true);
        	}
    		return null;
        }

        if (stack.getItem().onDroppedByPlayer(stack, p))
        {
            int count = p_71040_1_ && p.inventory.getCurrentItem() != null ? p.inventory.getCurrentItem().stackSize : 1;
            int slot = ((InventoryPlayerBattle)p.inventory).currentItem;
            slot = slot - InventoryPlayerBattle.OFFSET > 4 ? slot - InventoryPlayerBattle.OFFSET - 4: slot - InventoryPlayerBattle.OFFSET + 4;
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
		if (Minecraft.getMinecraft().thePlayer.inventory.getCurrentItem() == null || mc.objectMouseOver.typeOfHit == MovingObjectType.BLOCK || !BattlegearUtils.usagePriorAttack(Minecraft.getMinecraft().thePlayer.inventory.getCurrentItem())) {
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
				Minecraft.getMinecraft().thePlayer.inventory.currentItem -= InventoryPlayerBattle.WEAPON_SETS;
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
        boolean moreThan = ((InventoryPlayerBattle)entityclientplayermp.inventory).currentItem <= 153;
        ItemStack itemstack = ((InventoryPlayerBattle)entityclientplayermp.inventory).getStackInSlot(moreThan ? ((InventoryPlayerBattle)entityclientplayermp.inventory).currentItem : ((InventoryPlayerBattle)entityclientplayermp.inventory).currentItem - 4);

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
		if (((InventoryPlayerBattle)p_77615_3_.inventory).getCurrentOffhandWeapon() != null && ((InventoryPlayerBattle)p_77615_3_.inventory).getCurrentItem() != null && ((InventoryPlayerBattle)p_77615_3_.inventory).getCurrentOffhandWeapon().getItem() instanceof ItemBow && ((InventoryPlayerBattle)p_77615_3_.inventory).getCurrentItem().getItem() instanceof ItemBow) {
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
