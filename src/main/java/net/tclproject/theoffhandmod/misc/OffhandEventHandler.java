package net.tclproject.theoffhandmod.misc;

import java.util.ArrayList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Optional;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent.KeyInputEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mods.battlegear2.BattlemodeHookContainerClass;
import mods.battlegear2.api.core.BattlegearTranslator;
import mods.battlegear2.api.core.BattlegearUtils;
import mods.battlegear2.api.core.IBattlePlayer;
import mods.battlegear2.api.core.InventoryPlayerBattle;
import mods.battlegear2.client.BattlegearClientTickHandeler;
import mods.battlegear2.packet.BattlegearSyncItemPacket;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.Clone;
import net.tclproject.mysteriumlib.asm.fixes.MysteriumPatchesFixesO;
import net.tclproject.mysteriumlib.network.OFFMagicNetwork;
import net.tclproject.theoffhandmod.OffhandConfig;
import net.tclproject.theoffhandmod.TheOffhandMod;
import net.tclproject.theoffhandmod.packets.OverrideSyncServer;
import proxy.client.TOMClientProxy;

import static cpw.mods.fml.common.Loader.isModLoaded;

public class OffhandEventHandler {
   
   private boolean firstTick;

   /**The global state of the game, different for each world. Goes like this: six numbers for main questline progression, groups of three characters
    * for each quest, e.g. with 2 quests at the start of the game it'll be "000000001001", which can be broken down into "000000", "001" and "001"*/
   public static String globalState;
   private static boolean lastPressed = false;
   public static int ticks = 0;

   public OffhandEventHandler() {
   }
   
    @SideOnly(Side.CLIENT)
	@SubscribeEvent(priority=EventPriority.NORMAL, receiveCanceled=true)
	public void onKeyInputEvent(KeyInputEvent event)
	{
    	
		if (TOMClientProxy.keyBindReverseAction.isPressed()) {
			if (!lastPressed) {
				BattlegearUtils.reverseactionconfirmed = true;
				OFFMagicNetwork.dispatcher.sendToServer(new OverrideSyncServer(Minecraft.getMinecraft().thePlayer));
				lastPressed = true;
			}
		} else if (!TOMClientProxy.keyBindReverseAction.getIsKeyPressed() && lastPressed) {
			BattlegearUtils.reverseactionconfirmed = false;
			OFFMagicNetwork.dispatcher.sendToServer(new OverrideSyncServer(Minecraft.getMinecraft().thePlayer));
			lastPressed = false;
		}
	}
    
    public static boolean cancelone = false;

	public static int delay;
    
	@SideOnly(Side.CLIENT)
	@SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
	public void onClick(MouseEvent event) 
	{
    	OFFMagicNetwork.dispatcher.sendToServer(new OverrideSyncServer(Minecraft.getMinecraft().thePlayer));
		boolean shouldUndo = false;
		int leftclick = reversedAttack() ? 1 : 0;
		int rightclick = reversedUse() ? 0 : 1;
        if (Minecraft.getMinecraft().thePlayer.capabilities.allowEdit) {
            if (event.button == leftclick && event.buttonstate && !BattlegearUtils.reverseactionconfirmed && Minecraft.getMinecraft().thePlayer.inventory.getCurrentItem() != null && BattlemodeHookContainerClass.isItemBlock(Minecraft.getMinecraft().thePlayer.inventory.getCurrentItem().getItem())) {
                MovingObjectPosition mop = BattlemodeHookContainerClass.getRaytraceBlock(Minecraft.getMinecraft().thePlayer);
                if (mop != null && !canBlockBeInteractedWith(Minecraft.getMinecraft().theWorld, mop.blockX, mop.blockY, mop.blockZ)) {
                    cancelone = true;
                    boolean reversedIt = false;
                    if (!MysteriumPatchesFixesO.shouldNotOverride) reversedIt = true;
                    MysteriumPatchesFixesO.shouldNotOverride = true;
                    OFFMagicNetwork.dispatcher.sendToServer(new OverrideSyncServer(Minecraft.getMinecraft().thePlayer));
                    Minecraft.getMinecraft().func_147121_ag();
                    if (reversedIt) MysteriumPatchesFixesO.shouldNotOverride = false;
                    OFFMagicNetwork.dispatcher.sendToServer(new OverrideSyncServer(Minecraft.getMinecraft().thePlayer));
                }
            }
        }
		if(event.button == leftclick && !event.buttonstate && cancelone) {
			cancelone=false;
		}
		if(event.button == leftclick && !event.buttonstate && !MysteriumPatchesFixesO.leftclicked) shouldUndo = true;
		if(event.button == leftclick && !event.buttonstate && (MysteriumPatchesFixesO.leftclicked || shouldUndo)) {
			KeyBinding keyCode = Minecraft.getMinecraft().gameSettings.keyBindUseItem;
			KeyBinding.setKeyBindState(keyCode.getKeyCode(), false);
			MysteriumPatchesFixesO.leftclicked = false;
		}

		if(event.button == rightclick && event.buttonstate) {
			MysteriumPatchesFixesO.shouldNotOverride = false;
		}
	}
	
	// Used for if the user decides to rebind attack to right click in order to switch the controls around
	@SideOnly(Side.CLIENT)
	public static boolean reversedAttack() {
		return Minecraft.getMinecraft().gameSettings.keyBindAttack.getKeyCode() == -99;
	}
	
	// Used for if the user decides to rebind use to left click in order to switch the controls around
	@SideOnly(Side.CLIENT)
	public static boolean reversedUse() {
		return Minecraft.getMinecraft().gameSettings.keyBindUseItem.getKeyCode() == -100;
	}

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public void onUpdatePlayer(TickEvent.PlayerTickEvent event)
   {
	   if (firstTick) {
		   onFirstTick(event.player);
		   firstTick = false;
	   }

	   if (delay > 0) {
		   delay--;
	   }

	   ticks++;
	   if(ticks >= 30) {
		   ticks = 0;
		   if (BattlemodeHookContainerClass.tobeclosed.size() > 0) {
			   IInventory i = BattlemodeHookContainerClass.tobeclosed.get(0);
			   i.closeInventory();
			   BattlemodeHookContainerClass.tobeclosed.remove(i);
		   }
	   }

	   if (FMLCommonHandler.instance().getSide().isClient()) {
	       clientHelper(event);
       }

	   for (int i = 0; i < 4; ++i)
        {
			ItemStack itemstack = ((InventoryPlayerBattle)event.player.inventory).extraItems[InventoryPlayerBattle.OFFSET + i + 4 - InventoryPlayerBattle.OFFSET];
			if (itemstack != event.player.inventory.getStackInSlot(i)) {
				if (event.player.inventory.getStackInSlot(i) == null || event.player.inventory.getStackInSlot(i).stackSize == 0) {
					((InventoryPlayerBattle)event.player.inventory).setInventorySlotContents(InventoryPlayerBattle.OFFSET + i + 4, null);
					event.player.inventory.setInventorySlotContents(i, null);
				}
				else ((InventoryPlayerBattle)event.player.inventory).setInventorySlotContents(InventoryPlayerBattle.OFFSET + i + 4, event.player.inventory.getStackInSlot(i));
				event.player.inventory.markDirty();
			}
        }
	   for (int i = 4; i < 8; ++i)
       {
			ItemStack itemstack = ((InventoryPlayerBattle)event.player.inventory).extraItems[InventoryPlayerBattle.OFFSET + i - 4 - InventoryPlayerBattle.OFFSET];
			if (itemstack != event.player.inventory.getStackInSlot(i)) {
				if (event.player.inventory.getStackInSlot(i) == null || event.player.inventory.getStackInSlot(i).stackSize == 0) {
					((InventoryPlayerBattle)event.player.inventory).setInventorySlotContents(InventoryPlayerBattle.OFFSET + i - 4, null);
					event.player.inventory.setInventorySlotContents(i, null);
				}
				else ((InventoryPlayerBattle)event.player.inventory).setInventorySlotContents(InventoryPlayerBattle.OFFSET + i - 4, event.player.inventory.getStackInSlot(i));
				event.player.inventory.markDirty();
			}
       }
   }

   @SideOnly(Side.CLIENT)
    private void clientHelper(TickEvent.PlayerTickEvent event) {
        if (!MysteriumPatchesFixesO.shouldNotOverride && event.player.worldObj.isRemote && TheOffhandMod.proxy.getLeftClickCounter() <= 0) {
            if (event.player.capabilities.allowEdit) {
                if (TheOffhandMod.proxy.isRightClickHeld()) { // if it's a block and we should try break it
                    ItemStack mainHandItem = event.player.getCurrentEquippedItem();
                    ItemStack offhandItem = ((InventoryPlayerBattle) event.player.inventory).getCurrentOffhandWeapon();
                    MovingObjectPosition mop = BattlemodeHookContainerClass.getRaytraceBlock(event.player);
                    if (offhandItem != null && BattlemodeHookContainerClass.isItemBlock(offhandItem.getItem())) {
                        if (!BattlegearUtils.usagePriorAttack(offhandItem) && mop != null) {
                            BattlemodeHookContainerClass.tryBreakBlockOffhand(mop, offhandItem, mainHandItem, event);
                            TheOffhandMod.proxy.setLeftClickCounter(10);
                        } else {
                            Minecraft.getMinecraft().playerController.resetBlockRemoving();
                        }
                    } else {
                        if (mop != null && (!BattlegearUtils.usagePriorAttack(offhandItem) || BattlegearUtils.reverseactionconfirmed) && (!canBlockBeInteractedWith(Minecraft.getMinecraft().theWorld, mop.blockX, mop.blockY, mop.blockZ) || BattlegearUtils.reverseactionconfirmed)) {
                            BattlemodeHookContainerClass.tryBreakBlockOffhand(mop, offhandItem, mainHandItem, event);
                            TheOffhandMod.proxy.setLeftClickCounter(10);
                        } else {
                            Minecraft.getMinecraft().playerController.resetBlockRemoving();
                        }
                    }
                } else if (!TheOffhandMod.proxy.isLeftClickHeld()) {
                    Minecraft.getMinecraft().playerController.resetBlockRemoving();
                }
            }
        }
    }

    private static String[] activatedBlockMethodNames = {
           BattlegearTranslator.getMapedMethodName("Block", "func_149727_a", "onBlockActivated"),
           BattlegearTranslator.getMapedMethodName("Block", "func_149699_a", "onBlockClicked")};
   private static Class[][] activatedBlockMethodParams = {
               new Class[]{World.class, int.class, int.class, int.class, EntityPlayer.class, int.class, float.class, float.class, float.class},
               new Class[]{World.class, int.class, int.class, int.class, EntityPlayer.class}};

   @SuppressWarnings("unchecked")
   public static boolean canBlockBeInteractedWith(World worldObj, int x, int y, int z) {
	   if (worldObj == null) return false;
	   Block block = worldObj.getBlock(x, y, z);
	   if (block == null) return false;
	   if (block.getClass().equals(Block.class)) return false;
	   try {
           Class c = block.getClass();
           while (!(c.equals(Block.class))) {
               try {
                   try {
                       c.getDeclaredMethod(activatedBlockMethodNames[0], activatedBlockMethodParams[0]);
                       return true;
                   } catch (NoSuchMethodException ignored) {
                   }

                   try {
                       c.getDeclaredMethod(activatedBlockMethodNames[1], activatedBlockMethodParams[1]);
                       return true;
                   } catch (NoSuchMethodException ignored) {
                   }
               } catch (NoClassDefFoundError ignored) {

               }

               c = c.getSuperclass();
           }

           return false;
       } catch (NullPointerException e) {
           return true;
       }
   }

   private void onFirstTick(EntityPlayer p) {
	   
   }

	@SubscribeEvent
	public void onClonePlayer(Clone event) {

       if (!event.entityPlayer.worldObj.getGameRules().getGameRuleBooleanValue("keepInventory")) {
           ((InventoryPlayerBattle) event.entityPlayer.inventory).clearInventory(null, -1);
           event.entityPlayer.inventory.markDirty();
           event.entityPlayer.inventoryContainer.detectAndSendChanges();
       }

		((InventoryPlayerBattle)event.entityPlayer.inventory).currentItem = 150;
		TheOffhandMod.packetHandler.sendPacketToServer(new BattlegearSyncItemPacket(event.entityPlayer).generatePacket());
		TheOffhandMod.packetHandler.sendPacketToPlayer(new BattlegearSyncItemPacket(event.entityPlayer).generatePacket(), (EntityPlayerMP)event.entityPlayer);
        if (FMLCommonHandler.instance().getSide().isClient()) {
            syncPlayItem();
        }
	}

	@SideOnly(Side.CLIENT)
    private static void syncPlayItem() {
       Minecraft.getMinecraft().playerController.syncCurrentPlayItem();
    }
	
	@SideOnly(Side.CLIENT)
	@SubscribeEvent(
      priority = EventPriority.NORMAL
   )
	public void renderThirdPersonItemCustom(RenderPlayerEvent.Specials.Pre event) {
		event.renderItem = false;
		
		boolean lessThan = ((InventoryPlayerBattle)event.entityPlayer.inventory).currentItem <= 153;
		
        ItemStack itemstack1 = ((InventoryPlayerBattle)event.entityPlayer.inventory).getStackInSlot(lessThan ? ((InventoryPlayerBattle)event.entityPlayer.inventory).currentItem : ((InventoryPlayerBattle)event.entityPlayer.inventory).currentItem - BattlemodeHookContainerClass.prevOffhandOffset);

        if (itemstack1 != null)
        {
            GL11.glPushMatrix();
            event.renderer.modelBipedMain.bipedRightArm.postRender(0.0625F);
            GL11.glTranslatef(-0.0625F, 0.4375F, 0.0625F);

            if (event.entityPlayer.fishEntity != null)
            {
                itemstack1 = new ItemStack(Items.stick);
            }

            EnumAction enumaction = null;

            if (event.entityPlayer.getItemInUseCount() > 0)
            {
                enumaction = itemstack1.getItemUseAction();
            }

            net.minecraftforge.client.IItemRenderer customRenderer = net.minecraftforge.client.MinecraftForgeClient.getItemRenderer(itemstack1, net.minecraftforge.client.IItemRenderer.ItemRenderType.EQUIPPED);
            boolean is3D = (customRenderer != null && customRenderer.shouldUseRenderHelper(net.minecraftforge.client.IItemRenderer.ItemRenderType.EQUIPPED, itemstack1, net.minecraftforge.client.IItemRenderer.ItemRendererHelper.BLOCK_3D));

            float f2;
			if (is3D || itemstack1.getItem() instanceof ItemBlock && RenderBlocks.renderItemIn3d(Block.getBlockFromItem(itemstack1.getItem()).getRenderType()))
            {
                f2 = 0.5F;
                GL11.glTranslatef(0.0F, 0.1875F, -0.3125F);
                f2 *= 0.75F;
                GL11.glRotatef(20.0F, 1.0F, 0.0F, 0.0F);
                GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
                GL11.glScalef(-f2, -f2, f2);
            }
            else if (itemstack1.getItem() == Items.bow)
            {
                f2 = 0.625F;
                GL11.glTranslatef(0.0F, 0.125F, 0.3125F);
                GL11.glRotatef(-20.0F, 0.0F, 1.0F, 0.0F);
                GL11.glScalef(f2, -f2, f2);
                GL11.glRotatef(-100.0F, 1.0F, 0.0F, 0.0F);
                GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
            }
            else if (itemstack1.getItem().isFull3D())
            {
                f2 = 0.625F;

                if (itemstack1.getItem().shouldRotateAroundWhenRendering())
                {
                    GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);
                    GL11.glTranslatef(0.0F, -0.125F, 0.0F);
                }

                if (event.entityPlayer.getItemInUseCount() > 0 && enumaction == EnumAction.block)
                {
                    GL11.glTranslatef(0.05F, 0.0F, -0.1F);
                    GL11.glRotatef(-50.0F, 0.0F, 1.0F, 0.0F);
                    GL11.glRotatef(-10.0F, 1.0F, 0.0F, 0.0F);
                    GL11.glRotatef(-60.0F, 0.0F, 0.0F, 1.0F);
                }

                GL11.glTranslatef(0.0F, 0.1875F, 0.0F);
                GL11.glScalef(f2, -f2, f2);
                GL11.glRotatef(-100.0F, 1.0F, 0.0F, 0.0F);
                GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
            }
            else
            {
                f2 = 0.375F;
                GL11.glTranslatef(0.25F, 0.1875F, -0.1875F);
                GL11.glScalef(f2, f2, f2);
                GL11.glRotatef(60.0F, 0.0F, 0.0F, 1.0F);
                GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
                GL11.glRotatef(20.0F, 0.0F, 0.0F, 1.0F);
            }

            float f3;
            int k;
            float f12;

            if (itemstack1.getItem().requiresMultipleRenderPasses())
            {
                for (k = 0; k < itemstack1.getItem().getRenderPasses(itemstack1.getItemDamage()); ++k)
                {
                    int i = itemstack1.getItem().getColorFromItemStack(itemstack1, k);
                    f12 = (float)(i >> 16 & 255) / 255.0F;
                    f3 = (float)(i >> 8 & 255) / 255.0F;
                    float f4 = (float)(i & 255) / 255.0F;
                    GL11.glColor4f(f12, f3, f4, 1.0F);
                    event.renderer.renderManager.itemRenderer.renderItem(event.entityPlayer, itemstack1, k);
                }
            }
            else
            {
                k = itemstack1.getItem().getColorFromItemStack(itemstack1, 0);
                float f11 = (float)(k >> 16 & 255) / 255.0F;
                f12 = (float)(k >> 8 & 255) / 255.0F;
                f3 = (float)(k & 255) / 255.0F;
                GL11.glColor4f(f11, f12, f3, 1.0F);
                event.renderer.renderManager.itemRenderer.renderItem(event.entityPlayer, itemstack1, 0);
            }

            GL11.glPopMatrix();
        }
	}

	@SideOnly(Side.CLIENT)
	@SubscribeEvent(
      priority = EventPriority.NORMAL
   )
   public void renderHotbarOverlay(RenderGameOverlayEvent event) 
   {
	   if (event.isCancelable() || event.type != ElementType.EXPERIENCE)
		{
			return;
		}
	   
	   Minecraft mc = Minecraft.getMinecraft();

	   if (Minecraft.getMinecraft().thePlayer.capabilities.allowEdit) {
           if (Minecraft.getMinecraft().rightClickDelayTimer == 0 && Minecraft.getMinecraft().gameSettings.keyBindAttack.getIsKeyPressed() && !BattlegearUtils.reverseactionconfirmed && Minecraft.getMinecraft().thePlayer.inventory.getCurrentItem() != null && BattlemodeHookContainerClass.isItemBlock(Minecraft.getMinecraft().thePlayer.inventory.getCurrentItem().getItem())) { // places blocks continuously if left click is held with block in hand
               MovingObjectPosition mop = BattlemodeHookContainerClass.getRaytraceBlock(Minecraft.getMinecraft().thePlayer);
               if (mop != null && !canBlockBeInteractedWith(Minecraft.getMinecraft().theWorld, mop.blockX, mop.blockY, mop.blockZ)) {
                   boolean reversedIt = false;
                   if (!MysteriumPatchesFixesO.shouldNotOverride) reversedIt = true;
                   MysteriumPatchesFixesO.shouldNotOverride = true;
                   OFFMagicNetwork.dispatcher.sendToServer(new OverrideSyncServer(Minecraft.getMinecraft().thePlayer));
                   mc.func_147121_ag();
                   if (reversedIt) MysteriumPatchesFixesO.shouldNotOverride = false;
                   OFFMagicNetwork.dispatcher.sendToServer(new OverrideSyncServer(Minecraft.getMinecraft().thePlayer));
               }
           }
       }
	   
       if (!((IBattlePlayer) mc.thePlayer).isBattlemode()) {
    	   BattlegearClientTickHandeler.INSTANCE.previousNormal = mc.thePlayer.inventory.currentItem;
    	   mc.thePlayer.inventory.currentItem = BattlegearClientTickHandeler.INSTANCE.previousBattlemode;
    	   mc.playerController.syncCurrentPlayItem();

           BattlegearClientTickHandeler.INSTANCE.drawDone = true;
    	   BattlegearClientTickHandeler.INSTANCE.inBattle = true;
       }
	   renderHotbar(mc.ingameGUI, event.resolution.getScaledWidth(), event.resolution.getScaledHeight(), event.partialTicks);
	}

	@SideOnly(Side.CLIENT)
	protected void renderHotbar(GuiIngame gui, int width, int height, float partialTicks)
    {
		Minecraft mc = Minecraft.getMinecraft();
        mc.mcProfiler.startSection("actionBar");

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.renderEngine.bindTexture(new ResourceLocation("textures/gui/widgets.png"));

        gui.drawTexturedModalRect(width / 2 - 91, height - 22, 0, 0, 81, 22);
        gui.drawTexturedModalRect(width / 2 - 91 + 81, height - 22, 0, 0, 1, 22);

        gui.drawTexturedModalRect(width / 2 - 69 + 78, height - 22, 0, 0, 81, 22);
        gui.drawTexturedModalRect(width / 2 - 91 + 181, height - 22, 0, 0, 1, 22);
        
        if (OffhandConfig.nthSlot) {
        	gui.drawTexturedModalRect(width / 2 - 69 + 177, height - 22, 0, 0, 21, 22);
            gui.drawTexturedModalRect(width / 2 - 91 + 220, height - 22, 0, 0, 1, 22);
        }

        gui.drawTexturedModalRect(width / 2 - 91 - 1 + (mc.thePlayer.inventory.currentItem - InventoryPlayerBattle.OFFSET + 5 + (MysteriumPatchesFixesO.hotSwapped ? -BattlemodeHookContainerClass.prevOffhandOffset : 0)) * 20, height - 22 - 1, 0, 22, 24, 22);
        gui.drawTexturedModalRect(width / 2 - 91 - 1 + (((InventoryPlayerBattle)mc.thePlayer.inventory).currentItemInactive - InventoryPlayerBattle.WEAPON_SETS - InventoryPlayerBattle.OFFSET + (MysteriumPatchesFixesO.hotSwapped ? -((InventoryPlayerBattle)Minecraft.getMinecraft().thePlayer.inventory).getOffsetToInactiveHand() : 0)) * 20, height - 22 - 1, 0, 22, 24, 22);

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        RenderHelper.enableGUIStandardItemLighting();

        for (int i = 4; i < 8; ++i)
        {
            int x = width / 2 - 90 + (i - 4) * 20 + 2;
            int z = height - 16 - 3;
            renderInventorySlot(InventoryPlayerBattle.OFFSET + i, x, z, partialTicks);
        }

        for (int i = 0; i < 4; ++i)
        {
            int x = width / 2 - 90 + (i + 5) * 20 + 2;
            int z = height - 16 - 3;
            renderInventorySlot(InventoryPlayerBattle.OFFSET + i, x, z, partialTicks);
        }
        
        if (OffhandConfig.nthSlot) {
        	int i = 8;
            int x = width / 2 - 90 + (10) * 20 + 1;
            int z = height - 16 - 3;
            renderInventorySlot(InventoryPlayerBattle.OFFSET + i, x, z, partialTicks);
        }

        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        mc.mcProfiler.endSection();
    }

	@SideOnly(Side.CLIENT)
	protected void renderInventorySlot(int p_73832_1_, int p_73832_2_, int p_73832_3_, float p_73832_4_)
    {
		Minecraft mc = Minecraft.getMinecraft();
        ItemStack itemstack = ((InventoryPlayerBattle)mc.thePlayer.inventory).extraItems[p_73832_1_ - InventoryPlayerBattle.OFFSET];
        if (p_73832_1_ == 158) itemstack = ((InventoryPlayerBattle)mc.thePlayer.inventory).getStackInSlot(8);
        
        if (itemstack != null)
        {
            float f1 = itemstack.animationsToGo - p_73832_4_;

            if (f1 > 0.0F)
            {
                GL11.glPushMatrix();
                float f2 = 1.0F + f1 / 5.0F;
                GL11.glTranslatef(p_73832_2_ + 8, p_73832_3_ + 12, 0.0F);
                GL11.glScalef(1.0F / f2, (f2 + 1.0F) / 2.0F, 1.0F);
                GL11.glTranslatef((-(p_73832_2_ + 8)), (-(p_73832_3_ + 12)), 0.0F);
            }

            GuiIngame.itemRenderer.renderItemAndEffectIntoGUI(mc.fontRenderer, mc.getTextureManager(), itemstack, p_73832_2_, p_73832_3_);

            if (f1 > 0.0F)
            {
                GL11.glPopMatrix();
            }

            GuiIngame.itemRenderer.renderItemOverlayIntoGUI(mc.fontRenderer, mc.getTextureManager(), itemstack, p_73832_2_, p_73832_3_);
        }
    }
}
