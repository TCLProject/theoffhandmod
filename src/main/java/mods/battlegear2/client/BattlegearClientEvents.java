package mods.battlegear2.client;

import java.util.List;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import mods.battlegear2.api.RenderItemBarEvent;
import mods.battlegear2.api.core.IBattlePlayer;
import mods.battlegear2.api.core.InventoryPlayerBattle;
import mods.battlegear2.client.gui.BattlegearInGameGUI;
import mods.battlegear2.client.utils.BattlegearRenderHelper;
import net.minecraft.block.Block;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.tclproject.theoffhandmod.TheOffhandMod;

public final class BattlegearClientEvents {

	private final BattlegearInGameGUI inGameGUI;
    //public static final ResourceLocation patterns = new ResourceLocation("battlegear2", "textures/heraldry/Patterns-small.png");
    //public static int storageIndex;

    private static final int MAIN_INV = InventoryPlayer.getHotbarSize();
    public static final BattlegearClientEvents INSTANCE = new BattlegearClientEvents();

    private BattlegearClientEvents(){
        inGameGUI = new BattlegearInGameGUI();
    }

    /**
     * Offset battle slots rendering according to config values
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void postRenderBar(RenderItemBarEvent.BattleSlots event) {
        if(!event.isMainHand){
            event.xOffset += 0;
            event.yOffset += 0;
        }else{
            event.xOffset += 0;
            event.yOffset += 0;
        }
    }

    /**
     * Offset quiver slots rendering according to config values
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void postRenderQuiver(RenderItemBarEvent.QuiverSlots event) {
        event.xOffset += 0;
        event.yOffset += 0;
    }

    /**
     * Offset shield stamina rendering according to config values
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void postRenderShield(RenderItemBarEvent.ShieldBar event) {
        event.xOffset += 0;
        event.yOffset += 0;
    }

    /**
     * Render all the Battlegear HUD elements
     */
	@SubscribeEvent(receiveCanceled = true)
	public void postRenderOverlay(RenderGameOverlayEvent.Post event) {
		if (event.type == RenderGameOverlayEvent.ElementType.HOTBAR && (false || !event.isCanceled())) {
			inGameGUI.renderGameOverlay(event.partialTicks, event.mouseX, event.mouseY);
		}
	}

    /**
     * Bend the models when the item in left hand is used
     * And stop the right hand inappropriate bending
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void renderPlayerLeftItemUsage(RenderLivingEvent.Pre event){
        if(event.entity instanceof EntityPlayer) {
            EntityPlayer entityPlayer = (EntityPlayer) event.entity;
            ItemStack offhand = ((InventoryPlayerBattle) entityPlayer.inventory).getCurrentOffhandWeapon();
            if (offhand != null && event.renderer instanceof RenderPlayer) {
                RenderPlayer renderer = ((RenderPlayer) event.renderer);
                renderer.modelArmorChestplate.heldItemLeft = renderer.modelArmor.heldItemLeft = renderer.modelBipedMain.heldItemLeft = 1;
                if (entityPlayer.getItemInUseCount() > 0 && entityPlayer.getItemInUse() == offhand) {
                    EnumAction enumaction = offhand.getItemUseAction();
                    if (enumaction == EnumAction.block) {
                        renderer.modelArmorChestplate.heldItemLeft = renderer.modelArmor.heldItemLeft = renderer.modelBipedMain.heldItemLeft = 3;
                    } else if (enumaction == EnumAction.bow) {
                        renderer.modelArmorChestplate.aimedBow = renderer.modelArmor.aimedBow = renderer.modelBipedMain.aimedBow = true;
                    }
                    ItemStack mainhand = entityPlayer.inventory.getCurrentItem();
                    renderer.modelArmorChestplate.heldItemRight = renderer.modelArmor.heldItemRight = renderer.modelBipedMain.heldItemRight = mainhand != null ? 1 : 0;
                }else if(((IBattlePlayer)entityPlayer).isBlockingWithShield()){
                    renderer.modelArmorChestplate.heldItemLeft = renderer.modelArmor.heldItemLeft = renderer.modelBipedMain.heldItemLeft = 3;
                }
            }
        }
    }

    /**
     * Reset models to default values
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void resetPlayerLeftHand(RenderPlayerEvent.Post event){
        event.renderer.modelArmorChestplate.heldItemLeft = event.renderer.modelArmor.heldItemLeft = event.renderer.modelBipedMain.heldItemLeft = 0;
    }

    /**
     * Render a player left hand item, or sheathed items, and quiver on player back
     */
	@SubscribeEvent
	public void render3rdPersonBattlemode(RenderPlayerEvent.Specials.Post event) {

		ModelBiped biped = (ModelBiped) event.renderer.mainModel;
		BattlegearRenderHelper.renderItemIn3rdPerson(event.entityPlayer, biped, event.partialRenderTick);
	}

    /**
     * Fixes pick block, NOTE: doesn't appear to work with my mod - in my case, the default logic works fine from what I see
     */
//    @SubscribeEvent(priority = EventPriority.HIGHEST)
//    public void replacePickBlock(MouseEvent event){
//        if(event.buttonstate){
//            Minecraft mc = FMLClientHandler.instance().getClient();
//            if (mc.thePlayer != null) {
//                if(event.button-100 == mc.gameSettings.keyBindPickBlock.getKeyCode()){
//                    event.setCanceled(true);
//                    if (!((IBattlePlayer) mc.thePlayer).isBattlemode()) {
//                        boolean isCreative = mc.thePlayer.capabilities.isCreativeMode;
//                        ItemStack stack = getItemFromPointedAt(mc.objectMouseOver, mc.thePlayer);
//                        if (stack != null) {
//                            int k = -1;
//                            ItemStack temp;
//                            for (int slot = 0; slot < MAIN_INV; slot++) {
//                                temp = mc.thePlayer.inventory.getStackInSlot(slot);
//                                if (temp != null && stack.isItemEqual(temp) && ItemStack.areItemStackTagsEqual(stack, temp)) {
//                                    k = slot;
//                                    break;
//                                }
//                            }
//                            if (isCreative && k == -1) {
//                                k = mc.thePlayer.inventory.getFirstEmptyStack();
//                                if (k < 0 || k >= MAIN_INV) {
//                                    k = mc.thePlayer.inventory.currentItem;
//                                }
//                            }
//                            if (k >= 0 && k < MAIN_INV) {
//                                mc.thePlayer.inventory.currentItem = k;
//                                TheOffhandMod.packetHandler.sendPacketToServer(new PickBlockPacket(stack, k).generatePacket());
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }

    /**
     * Equivalent code to the creative pick block
     * @param target The client target vector
     * @param player The player trying to pick
     * @return the stack expected for the creative pick button
     */
    private static ItemStack getItemFromPointedAt(MovingObjectPosition target, EntityPlayer player) {
        if(target!=null){
            if (target.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK)
            {
                int x = target.blockX;
                int y = target.blockY;
                int z = target.blockZ;
                World world = player.getEntityWorld();
                Block block = world.getBlock(x, y, z);
                if (block.isAir(world, x, y, z))
                {
                    return null;
                }
                return block.getPickBlock(target, world, x, y, z);//TODO support newer version
            }
            else
            {
                if (target.typeOfHit != MovingObjectPosition.MovingObjectType.ENTITY || target.entityHit == null || !player.capabilities.isCreativeMode)
                {
                    return null;
                }
                return target.entityHit.getPickedResult(target);
            }
        }
        return null;
    }

	@SubscribeEvent(priority = EventPriority.LOW)
    public void postInitGui(GuiScreenEvent.InitGuiEvent.Post event){
        if (TheOffhandMod.battlegearEnabled && event.gui instanceof InventoryEffectRenderer) {
            onOpenGui(event.buttonList, guessGuiLeft((InventoryEffectRenderer) event.gui)-30, guessGuiTop(event.gui));
        }
    }

    /**
     * Make a guess over the value of GuiContainer#guiLeft (protected)
     * Use magic numbers !
     * NotEnoughItems mod is also changing the gui offset, for some reason.
     *
     * @param guiContainer the current screen whose value is desired
     * @return the guessed value
     */
    public static int guessGuiLeft(InventoryEffectRenderer guiContainer){
        int offset = Loader.isModLoaded("NotEnoughItems") || FMLClientHandler.instance().getClientPlayerEntity().getActivePotionEffects().isEmpty() ? 0 : 60;
        if(guiContainer instanceof GuiContainerCreative){
            return offset + (guiContainer.width - 195)/2;
        }
        return offset + (guiContainer.width - 176)/2;
    }

    /**
     * Make a guess over the value of GuiContainer#guiTop (protected)
     * Use magic numbers !
     *
     * @param gui the current screen whose value is desired
     * @return the guessed value
     */
    public static int guessGuiTop(GuiScreen gui){
        if(gui instanceof GuiContainerCreative){
            return (gui.height - 136)/2;
        }
        return (gui.height - 166)/2;
    }

    /**
     * Helper method to add buttons to a gui when opened
     * @param buttons the List<GuiButton> of the opened gui
     * @param guiLeft horizontal placement parameter
     * @param guiTop vertical placement parameter
     */
	public static void onOpenGui(List buttons, int guiLeft, int guiTop) {
//        if(BattlegearConfig.enableGuiButtons){
//			int count = 0;
//			for (GuiPlaceableButton tab : tabsList) {
//                GuiPlaceableButton button = tab.copy();
//				button.place(count, guiLeft, guiTop);
//				button.id = buttons.size()+2;//Due to GuiInventory and GuiContainerCreative button performed actions, without them having buttons...
//				count++;
//				buttons.add(button);
//			}
//        }
	}
}
