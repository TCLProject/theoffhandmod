package mods.battlegear2.client;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mods.battlegear2.BattlemodeHookContainerClass;
import mods.battlegear2.api.PlayerEventChild;
import mods.battlegear2.api.core.BattlegearUtils;
import mods.battlegear2.api.core.IBattlePlayer;
import mods.battlegear2.api.core.IOffhandRender;
import mods.battlegear2.api.core.InventoryPlayerBattle;
import mods.battlegear2.api.shield.IShield;
import mods.battlegear2.api.weapons.IExtendedReachWeapon;
import mods.battlegear2.packet.BattlegearAnimationPacket;
import mods.battlegear2.packet.BattlegearShieldBlockPacket;
import mods.battlegear2.packet.OffhandPlaceBlockPacket;
import mods.battlegear2.utils.EnumBGAnimations;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.tclproject.mysteriumlib.asm.fixes.MysteriumPatchesFixesO;
import net.tclproject.theoffhandmod.TheOffhandMod;

public final class BattlegearClientTickHandeler {
	public static final int FLASH_MAX = 30;
    // TODO: Add special action to some items, maybe?
    public final KeyBinding special;
    public final Minecraft mc;

    public float blockBar = 1;
    public float partialTick;
    public boolean wasBlocking = false;
    public int previousBattlemode = InventoryPlayerBattle.OFFSET;
    public int previousNormal = 0;
    public int flashTimer;
    public boolean specialDone = false, drawDone = false, inBattle = false;
    public static final BattlegearClientTickHandeler INSTANCE = new BattlegearClientTickHandeler();

    private BattlegearClientTickHandeler(){
        special = new KeyBinding("Special", Keyboard.KEY_Z, "key.categories.gameplay");
        ClientRegistry.registerKeyBinding(special);
        mc = FMLClientHandler.instance().getClient();
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void keyDown(TickEvent.ClientTickEvent event) {
        if(TheOffhandMod.battlegearEnabled){
            //null checks to prevent any crash outside the world (and to make sure we have no screen open)
            if (mc.thePlayer != null && mc.theWorld != null && mc.currentScreen == null) {
                EntityClientPlayerMP player = mc.thePlayer;
                if(event.phase == TickEvent.Phase.START) {
                    if (!specialDone && special.getIsKeyPressed() && ((IBattlePlayer) player).getSpecialActionTimer() == 0) {

                        if (((IBattlePlayer) player).isBattlemode()) {
                            ItemStack offhand = ((InventoryPlayerBattle) player.inventory).getCurrentOffhandWeapon();

                            if (offhand != null && offhand.getItem() instanceof IShield) {
                                float shieldBashPenalty = 0;

                                if (blockBar >= shieldBashPenalty) {
                                    FMLProxyPacket p = new BattlegearAnimationPacket(EnumBGAnimations.SpecialAction, player).generatePacket();
                                    TheOffhandMod.packetHandler.sendPacketToServer(p);
                                    ((IBattlePlayer) player).setSpecialActionTimer(((IShield) offhand.getItem()).getBashTimer(offhand));

                                    blockBar -= shieldBashPenalty;
                                }
                            }
                        }
                        specialDone = true;
                    } else if (specialDone && !special.getIsKeyPressed()) {
                        specialDone = false;
                    }
                    inBattle = ((IBattlePlayer) player).isBattlemode();
                }else {
                    if(inBattle && !((IBattlePlayer) player).isBattlemode()){
                        for (int i = 0; i < InventoryPlayerBattle.WEAPON_SETS; ++i){
                            if (mc.gameSettings.keyBindsHotbar[i].getIsKeyPressed()){
                                previousBattlemode = InventoryPlayerBattle.OFFSET + i;
                            }
                        }
                        player.inventory.currentItem = previousBattlemode;
                        mc.playerController.syncCurrentPlayItem();
                    }
                }
            }
        }
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onPlayerTick(TickEvent.PlayerTickEvent event){
        if(event.player == mc.thePlayer) {
            if (event.phase == TickEvent.Phase.START) {
                tickStart(mc.thePlayer);
            } else {
                tickEnd(mc.thePlayer);
            }
        }
    }

    @SideOnly(Side.CLIENT)
    public void tickStart(EntityPlayer player) {
        if(((IBattlePlayer)player).isBattlemode()){
            ItemStack offhand = ((InventoryPlayerBattle) player.inventory).getCurrentOffhandWeapon();
            if(offhand != null){
                if(offhand.getItem() instanceof IShield){
                    if(flashTimer == FLASH_MAX){
                        player.motionY = player.motionY/2;
                    }
                    if(flashTimer > 0){
                        flashTimer --;
                    }
                    if(mc.gameSettings.keyBindUseItem.getIsKeyPressed() && !player.isSwingInProgress){
                        blockBar -= ((IShield) offhand.getItem()).getDecayRate(offhand);
                        if(blockBar > 0){
                            if(!wasBlocking){
                                TheOffhandMod.packetHandler.sendPacketToServer(new BattlegearShieldBlockPacket(true, player).generatePacket());
                            }
                            wasBlocking = true;
                        }else{
                            if(wasBlocking){
                                //Send packet
                                TheOffhandMod.packetHandler.sendPacketToServer(new BattlegearShieldBlockPacket(false, player).generatePacket());
                            }
                            wasBlocking = false;
                            blockBar = 0;
                        }
                    }else{
                        if(wasBlocking){
                            //send packet
                            TheOffhandMod.packetHandler.sendPacketToServer(new BattlegearShieldBlockPacket(false, player).generatePacket());
                        }
                        wasBlocking = false;
                        blockBar += ((IShield) offhand.getItem()).getRecoveryRate(offhand);
                        if(blockBar > 1){
                            blockBar = 1;
                        }
                    }
                }else if(!MysteriumPatchesFixesO.shouldNotOverride && mc.gameSettings.keyBindUseItem.getIsKeyPressed() && mc.rightClickDelayTimer == 4 && !player.isUsingItem()){
                    tryCheckUseItem(offhand, player);
                }
            }
        }
    }

    @SideOnly(Side.CLIENT)
    public void tryCheckUseItem(ItemStack offhand, EntityPlayer player){
        if(BattlegearUtils.usagePriorAttack(offhand) && !MysteriumPatchesFixesO.shouldNotOverride){
            MovingObjectPosition mouseOver = mc.objectMouseOver;
            boolean flag = true;
            if (mouseOver != null)
            {
                if (mouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY)
                {
                    if(mc.playerController.interactWithEntitySendPacket(player, mouseOver.entityHit))
                        flag = false;
                }
                else if (mouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK)
                {
                    int j = mouseOver.blockX;
                    int k = mouseOver.blockY;
                    int l = mouseOver.blockZ;
                    if (!player.worldObj.getBlock(j, k, l).isAir(player.worldObj, j, k, l)) {
                        final int size = offhand.stackSize;
                        int i1 = mouseOver.sideHit;
                        PlayerEventChild.UseOffhandItemEvent useItemEvent = new PlayerEventChild.UseOffhandItemEvent(new PlayerInteractEvent(player, PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK, j, k, l, i1, player.worldObj), offhand);
                        if (!MinecraftForge.EVENT_BUS.post(useItemEvent) && onPlayerPlaceBlock(mc.playerController, player, offhand, j, k, l, i1, mouseOver.hitVec)) {
                            ((IBattlePlayer) player).swingOffItem();
                            flag = false;
                        }
                        if (offhand == null)
                        {
                            return;
                        }
                        if (offhand.stackSize == 0)
                        {
                            BattlegearUtils.setPlayerOffhandItem(player, null);
                        }
                        else if (offhand.stackSize != size || mc.playerController.isInCreativeMode())
                        {
                            ((IOffhandRender)mc.entityRenderer.itemRenderer).setEquippedProgress(0.0F);
                        }
                    }
                }
            }
            if (flag)
            {
                offhand = ((InventoryPlayerBattle) player.inventory).getCurrentOffhandWeapon();
                PlayerEventChild.UseOffhandItemEvent useItemEvent = new PlayerEventChild.UseOffhandItemEvent(new PlayerInteractEvent(player, PlayerInteractEvent.Action.RIGHT_CLICK_AIR, 0, 0, 0, -1, player.worldObj), offhand);
                if (offhand != null && !MinecraftForge.EVENT_BUS.post(useItemEvent) && BattlemodeHookContainerClass.tryUseItem(player, offhand, Side.CLIENT))
                {
                    ((IOffhandRender)mc.entityRenderer.itemRenderer).setEquippedProgress(0.0F);
                }
            }
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event){
        if(event.phase == TickEvent.Phase.START){
            partialTick = event.renderTickTime;
            if(mc.currentScreen instanceof GuiMainMenu){
                TheOffhandMod.battlegearEnabled = false;
            }
        }
    }

    private boolean onPlayerPlaceBlock(PlayerControllerMP controller, EntityPlayer player, ItemStack offhand, int i, int j, int k, int l, Vec3 hitVec) {
        float f = (float)hitVec.xCoord - i;
        float f1 = (float)hitVec.yCoord - j;
        float f2 = (float)hitVec.zCoord - k;
        boolean flag = false;
        int i1;
        final World worldObj = player.worldObj;
        if (offhand.getItem().onItemUseFirst(offhand, player, worldObj, i, j, k, l, f, f1, f2)){
            return true;
        }
        if (!player.isSneaking() || ((InventoryPlayerBattle) player.inventory).getCurrentOffhandWeapon() == null || ((InventoryPlayerBattle) player.inventory).getCurrentOffhandWeapon().getItem().doesSneakBypassUse(worldObj, i, j, k, player)){
            Block b = worldObj.getBlock(i, j, k);
            if (!b.isAir(worldObj, i, j, k) && b.onBlockActivated(worldObj, i, j, k, player, l, f, f1, f2)){
                flag = true;
            }
        }
        if (!flag && offhand.getItem() instanceof ItemBlock){
            ItemBlock itemblock = (ItemBlock)offhand.getItem();
            if (!itemblock.func_150936_a(worldObj, i, j, k, l, player, offhand)){
                return false;
            }
        }
        TheOffhandMod.packetHandler.sendPacketToServer(new OffhandPlaceBlockPacket(i, j, k, l, offhand, f, f1, f2).generatePacket());
        if (flag){
            return true;
        }
        else if (offhand == null){
            return false;
        }
        else{
            if (controller.isInCreativeMode()){
                i1 = offhand.getItemDamage();
                int j1 = offhand.stackSize;
                boolean flag1 = offhand.tryPlaceItemIntoWorld(player, worldObj, i, j, k, l, f, f1, f2);
                offhand.setItemDamage(i1);
                offhand.stackSize = j1;
                return flag1;
            }
            else{
                if (!offhand.tryPlaceItemIntoWorld(player, worldObj, i, j, k, l, f, f1, f2)){
                    return false;
                }
                if (offhand.stackSize <= 0){
                    ForgeEventFactory.onPlayerDestroyItem(player, offhand);
                }
                return true;
            }
        }
    }

    @SideOnly(Side.CLIENT)
    public void tickEnd(EntityPlayer player) {
        ItemStack offhand = ((InventoryPlayerBattle) player.inventory).getCurrentOffhandWeapon();
        //If we use a shield
        if(offhand != null && offhand.getItem() instanceof IShield){
            if(mc.gameSettings.keyBindUseItem.getIsKeyPressed() && !player.isSwingInProgress && blockBar > 0){
                player.motionX = player.motionX/5;
                player.motionZ = player.motionZ/5;
            }
        }

        //If we JUST swung an Item
        if (player.swingProgressInt == 1) {    
            ItemStack mainhand = player.getCurrentEquippedItem();
            if (mainhand != null && mainhand.getItem() instanceof IExtendedReachWeapon) {
                float extendedReach = ((IExtendedReachWeapon) mainhand.getItem()).getReachModifierInBlocks(mainhand);
                if(extendedReach > 0){
                    MovingObjectPosition mouseOver = TheOffhandMod.proxy.getMouseOver(partialTick, extendedReach + mc.playerController.getBlockReachDistance());
                    if (mouseOver != null && mouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
                        Entity target = mouseOver.entityHit;
                        if (target instanceof EntityLivingBase && target != player && player.getDistanceToEntity(target) > mc.playerController.getBlockReachDistance()) {
                            if (target.hurtResistantTime != ((EntityLivingBase) target).maxHurtResistantTime) {
                                mc.playerController.attackEntity(player, target);
                            }
                        }
                    }
                }
            }
        }
    }

    public static void resetFlash(){
        INSTANCE.flashTimer = FLASH_MAX;
    }

    public static int getFlashTimer(){
        return INSTANCE.flashTimer;
    }

    public static float getBlockTime(){
        return INSTANCE.blockBar;
    }

    public static void reduceBlockTime(float value){
        INSTANCE.blockBar -= value;
    }

    public static float getPartialTick(){
        return INSTANCE.partialTick;
    }

    public static ItemStack getPreviousMainhand(EntityPlayer player){
        return player.inventory.getStackInSlot(INSTANCE.previousBattlemode);
    }

    public static ItemStack getPreviousOffhand(EntityPlayer player){
        return player.inventory.getStackInSlot(INSTANCE.previousBattlemode+InventoryPlayerBattle.WEAPON_SETS);
    }
}
