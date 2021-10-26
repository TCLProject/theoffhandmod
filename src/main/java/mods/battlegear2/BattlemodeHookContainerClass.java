package mods.battlegear2;

import java.util.ArrayList;
import java.util.List;

import static cpw.mods.fml.common.eventhandler.Event.Result.DEFAULT;
import static cpw.mods.fml.common.eventhandler.Event.Result.DENY;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.PlayerTickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mods.battlegear2.api.IHandListener;
import mods.battlegear2.api.IOffhandDual;
import mods.battlegear2.api.PlayerEventChild;
import mods.battlegear2.api.core.BattlegearUtils;
import mods.battlegear2.api.core.IBattlePlayer;
import mods.battlegear2.api.core.InventoryExceptionEvent;
import mods.battlegear2.api.core.InventoryPlayerBattle;
import mods.battlegear2.api.shield.IArrowCatcher;
import mods.battlegear2.api.shield.IShield;
import mods.battlegear2.api.weapons.IExtendedReachWeapon;
import mods.battlegear2.packet.BattlegearShieldFlashPacket;
import mods.battlegear2.packet.BattlegearSyncItemPacket;
import mods.battlegear2.packet.OffhandPlaceBlockPacket;
import mods.battlegear2.utils.EnumBGAnimations;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IProjectile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemBucket;
import net.minecraft.item.ItemDoor;
import net.minecraft.item.ItemRedstone;
import net.minecraft.item.ItemReed;
import net.minecraft.item.ItemSeedFood;
import net.minecraft.item.ItemSign;
import net.minecraft.item.ItemSkull;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.tclproject.mysteriumlib.asm.fixes.MysteriumPatchesFixesO;
import net.tclproject.theoffhandmod.TheOffhandMod;
import net.tclproject.theoffhandmod.misc.OffhandEventHandler;

public final class BattlemodeHookContainerClass {

    public static final BattlemodeHookContainerClass INSTANCE = new BattlemodeHookContainerClass();

    private BattlemodeHookContainerClass(){}

    private boolean isFake(Entity entity){
        return entity instanceof FakePlayer;
    }
    /**
     * Crash the game if our inventory has been replaced by something else, or the coremod failed
     * Also synchronize battle inventory
     * @param event that spawned the player
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onEntityJoin(EntityJoinWorldEvent event){
        if (event.entity instanceof EntityPlayer && !(isFake(event.entity))) {
            if (!(((EntityPlayer) event.entity).inventory instanceof InventoryPlayerBattle) && !MinecraftForge.EVENT_BUS.post(new InventoryExceptionEvent((EntityPlayer)event.entity))) {
                throw new RuntimeException("Player inventory has been replaced with " + ((EntityPlayer) event.entity).inventory.getClass());
            }
            if(event.entity instanceof EntityPlayerMP){
            	TheOffhandMod.packetHandler.sendPacketToPlayer(
                        new BattlegearSyncItemPacket((EntityPlayer) event.entity).generatePacket(),
                        (EntityPlayerMP) event.entity);

            }
        }
    }

    /**
     * Cancel the attack if the player reach is lowered by some types of items, or if barehanded
     * Note: Applies to either hands, since item is hotswap before this event for offhand weapons
     * @param event for the player attacking an entity
     */
    @SubscribeEvent
    public void attackEntity(AttackEntityEvent event){
        if(((IBattlePlayer) event.entityPlayer).getSpecialActionTimer() > 0){
            event.setCanceled(true);
            return;
        }

        ItemStack mainhand = event.entityPlayer.getCurrentEquippedItem();
        float reachMod = 0;
        if(mainhand == null)
            reachMod = -2.2F;//Reduce bare hands range
        else if(mainhand.getItem() instanceof ItemBlock)
            reachMod = -2.1F;//Reduce block in hands range too
        else if(mainhand.getItem() instanceof IExtendedReachWeapon)
            reachMod = ((IExtendedReachWeapon) mainhand.getItem()).getReachModifierInBlocks(mainhand);
        if(reachMod < 0 && reachMod + (event.entityPlayer.capabilities.isCreativeMode?5.0F:4.5F) < event.entityPlayer.getDistanceToEntity(event.target)){
            event.setCanceled(true);
        }
    }
    
    public static MovingObjectPosition getRaytraceBlock(EntityPlayer p) {
    	float scaleFactor = 1.0F;
		float rotPitch = p.prevRotationPitch + (p.rotationPitch - p.prevRotationPitch) * scaleFactor;
		float rotYaw = p.prevRotationYaw + (p.rotationYaw - p.prevRotationYaw) * scaleFactor;
		double testX = p.prevPosX + (p.posX - p.prevPosX) * scaleFactor;
		double testY = p.prevPosY + (p.posY - p.prevPosY) * scaleFactor + 1.62D - p.yOffset;//1.62 is player eye height
		double testZ = p.prevPosZ + (p.posZ - p.prevPosZ) * scaleFactor;
		Vec3 testVector = Vec3.createVectorHelper(testX, testY, testZ);
		float var14 = MathHelper.cos(-rotYaw * 0.017453292F - (float)Math.PI);
		float var15 = MathHelper.sin(-rotYaw * 0.017453292F - (float)Math.PI);
		float var16 = -MathHelper.cos(-rotPitch * 0.017453292F);
		float vectorY = MathHelper.sin(-rotPitch * 0.017453292F);
		float vectorX = var15 * var16;
		float vectorZ = var14 * var16;
		double reachLength = 5.0D;
		Vec3 testVectorFar = testVector.addVector(vectorX * reachLength, vectorY * reachLength, vectorZ * reachLength);
		return p.worldObj.rayTraceBlocks(testVector, testVectorFar, false);
    }
    
    public static List<IInventory> tobeclosed = new ArrayList<IInventory>();

    @SubscribeEvent
    public void playerInteract(PlayerInteractEvent event) {
        if(isFake(event.entityPlayer))
            return;
        if(((IBattlePlayer) event.entityPlayer).getSpecialActionTimer() > 0){
            event.setCanceled(true);
            event.entityPlayer.isSwingInProgress = false;
        }else if(((IBattlePlayer) event.entityPlayer).isBattlemode()) {
            if(event.action == PlayerInteractEvent.Action.RIGHT_CLICK_AIR || event.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {//Right click
                ItemStack mainHandItem = event.entityPlayer.getCurrentEquippedItem();
                ItemStack offhandItem = ((InventoryPlayerBattle) event.entityPlayer.inventory).getCurrentOffhandWeapon();
                if (!MysteriumPatchesFixesO.shouldNotOverride) {
                	PlayerInteractEvent.Result blk = event.useBlock;
                	PlayerInteractEvent.Result itm = event.useItem;
                	event.useBlock = PlayerInteractEvent.Result.DENY;
        			MovingObjectPosition mop = getRaytraceBlock(event.entityPlayer);
        			if (TheOffhandMod.proxy.isRightClickHeld() && !MysteriumPatchesFixesO.leftclicked) {
        				event.setCanceled(true);
        			}
        			if (mop != null) {
                    	event.setCanceled(true);
	        			int 
	        			i = mop.blockX,
	        			j = mop.blockY,
	        			k = mop.blockZ,
	        			side = mop.sideHit;
	                    float f = (float)mop.hitVec.xCoord - i;
	                    float f1 = (float)mop.hitVec.yCoord - j;
	                    float f2 = (float)mop.hitVec.zCoord - k;
	                    
	                    if (!event.entityPlayer.isSneaking() && !BattlegearUtils.reverseactionconfirmed && OffhandEventHandler.canBlockBeInteractedWith(event.entityPlayer.worldObj, i, j, k)) {
	                    	event.setCanceled(false);
	                    	event.useBlock = blk;
	                    	event.useItem = itm;
	                    }
        			}
                	if (event.entityPlayer.worldObj.isRemote && !(BattlegearUtils.reverseactionconfirmed && BattlegearUtils.usagePriorAttack(offhandItem))) sendOffSwingEvent(event, mainHandItem, offhandItem);
                }
            }else {//Left click
                ItemStack mainHandItem = event.entityPlayer.getCurrentEquippedItem();
                if(mainHandItem!=null && mainHandItem.getItem() instanceof IHandListener){
                    //TODO Test the following
                    /*event.useItem = Event.Result.DENY;
                    PlayerInteractEvent copy = new PlayerInteractEvent(event.entityPlayer, event.action, event.x, event.y, event.z, event.face, event.world);
                    Event.Result swing = ((IHandListener) mainHandItem.getItem()).onClickBlock(copy, mainHandItem, ((InventoryPlayerBattle) event.entityPlayer.inventory).getCurrentOffhandWeapon(), false);
                    if(swing != Event.Result.DEFAULT){
                        event.entityPlayer.isSwingInProgress = false;
                    }
                    if(swing == Event.Result.DENY){
                        event.setCanceled(true);
                        event.useBlock = copy.useBlock;
                    }*/
                }
            }
        }
    }
    
    public static boolean changedHeldItemTooltips = false;
    // used in hostwapping the item to dig with, to remember where to return the main slot to
    public static int prevOffhandOffset;
    
    public static boolean isItemBlock(Item item) {
    	return item instanceof ItemBlock || item instanceof ItemDoor || item instanceof ItemSign || item instanceof ItemReed || item instanceof ItemSeedFood || item instanceof ItemRedstone || item instanceof ItemBucket || item instanceof ItemSkull;
    }
    
    @SideOnly(Side.CLIENT)
    public static void tryBreakBlockOffhand(MovingObjectPosition objectMouseOver, ItemStack offhandItem, ItemStack mainHandItem, PlayerTickEvent event) {
    	Minecraft mcInstance = Minecraft.getMinecraft();
        int i = objectMouseOver.blockX;
        int j = objectMouseOver.blockY;
        int k = objectMouseOver.blockZ;
        if (mcInstance.thePlayer.capabilities.isCreativeMode) 
        {
        	if (OffhandEventHandler.delay <= 0) {
            	mcInstance.getNetHandler().addToSendQueue(new C07PacketPlayerDigging(0, i, j, k, objectMouseOver.sideHit));
            	PlayerControllerMP.clickBlockCreative(mcInstance, mcInstance.playerController, i, j, k, objectMouseOver.sideHit);
            	if (!(event.player.worldObj.isRemote && !(BattlegearUtils.reverseactionconfirmed && BattlegearUtils.usagePriorAttack(offhandItem)))) {
            		sendOffSwingEventNoCheck(mainHandItem, offhandItem); // force offhand swing anyway because we broke a block
                }
            	OffhandEventHandler.delay = 24;
        	}
        	return;
        }
        if (mcInstance.theWorld.getBlock(i, j, k).getMaterial() != Material.air)
        {
        	if (mcInstance.playerController.blockHitDelay > 0)
            {
                --mcInstance.playerController.blockHitDelay;
            }
            else
            {
            	mcInstance.playerController.isHittingBlock = true;
                mcInstance.playerController.currentBlockX = i;
                mcInstance.playerController.currentBlockY = j;
                mcInstance.playerController.currentblockZ = k;

            	if ((!ItemStack.areItemStacksEqual(((InventoryPlayerBattle)mcInstance.thePlayer.inventory).getCurrentOffhandWeapon(), mcInstance.thePlayer.inventory.getStackInSlot(mcInstance.thePlayer.inventory.currentItem))) && (((InventoryPlayerBattle)mcInstance.thePlayer.inventory).getCurrentOffhandWeapon() != null)) 
            	{
            		if (mcInstance.gameSettings.heldItemTooltips) {
            			mcInstance.gameSettings.heldItemTooltips = false;
            			changedHeldItemTooltips = true;   			
            		}
            		
            		prevOffhandOffset = ((InventoryPlayerBattle)mcInstance.thePlayer.inventory).getOffsetToInactiveHand();
                    mcInstance.thePlayer.inventory.currentItem += ((InventoryPlayerBattle)mcInstance.thePlayer.inventory).getOffsetToInactiveHand();
            		mcInstance.playerController.currentItemHittingBlock = ((InventoryPlayerBattle)mcInstance.thePlayer.inventory).getCurrentOffhandWeapon();
                    mcInstance.playerController.syncCurrentPlayItem();
                    MysteriumPatchesFixesO.hotSwapped = true;
            	}

                
                Block block = mcInstance.theWorld.getBlock(i, j, k);
                if (block.getMaterial() == Material.air)
                {
                    mcInstance.playerController.isHittingBlock = false;
                    return;
                }
                MysteriumPatchesFixesO.countToCancel = 5;
                mcInstance.playerController.curBlockDamageMP += block.getPlayerRelativeBlockHardness(mcInstance.thePlayer, mcInstance.thePlayer.worldObj, i, j, k);

                if (mcInstance.playerController.stepSoundTickCounter % 4.0F == 0.0F)
                {
                	mcInstance.getSoundHandler().playSound(new PositionedSoundRecord(new ResourceLocation(block.stepSound.getStepResourcePath()), (block.stepSound.getVolume() + 1.0F) / 8.0F, block.stepSound.getPitch() * 0.5F, (float)i + 0.5F, (float)j + 0.5F, (float)k + 0.5F));
                }

                ++mcInstance.playerController.stepSoundTickCounter;

                if (mcInstance.playerController.curBlockDamageMP >= 1.0F)
                {
                    
                    ItemStack itemstack = mcInstance.thePlayer.getCurrentEquippedItem();

                    if (itemstack != null)
                    {
                    	itemstack.func_150999_a(mcInstance.theWorld, block, i, j, k, mcInstance.thePlayer);

                        if (itemstack.stackSize == 0)
                        {
                        	mcInstance.thePlayer.destroyCurrentEquippedItem();
                        }
                    }
                    mcInstance.playerController.isHittingBlock = false;
                    mcInstance.getNetHandler().addToSendQueue(new C07PacketPlayerDigging(2, i, j, k, objectMouseOver.sideHit));
                    mcInstance.playerController.onPlayerDestroyBlock(i, j, k, objectMouseOver.sideHit);
                    mcInstance.playerController.curBlockDamageMP = 0.0F;
                    mcInstance.playerController.stepSoundTickCounter = 0.0F;
                    mcInstance.playerController.blockHitDelay = 5;
                    
        			if (MysteriumPatchesFixesO.hotSwapped) {
        				mcInstance.thePlayer.inventory.currentItem -= prevOffhandOffset;
        	            mcInstance.playerController.syncCurrentPlayItem();
        	            MysteriumPatchesFixesO.hotSwapped = false;
        			}
                }
                mcInstance.theWorld.destroyBlockInWorldPartially(mcInstance.thePlayer.getEntityId(), mcInstance.playerController.currentBlockX, mcInstance.playerController.currentBlockY, mcInstance.playerController.currentblockZ, (int)(mcInstance.playerController.curBlockDamageMP * 10.0F) - 1);
            }

            if (mcInstance.thePlayer.isCurrentToolAdventureModeExempt(i, j, k))
            {
            	mcInstance.effectRenderer.addBlockHitEffects(i, j, k, objectMouseOver);
            }
            if (!(event.player.worldObj.isRemote && !(BattlegearUtils.reverseactionconfirmed && BattlegearUtils.usagePriorAttack(offhandItem)))) {
            	sendOffSwingEventNoCheck(mainHandItem, offhandItem); // force offhand swing anyway because we broke a block
            }
        }
	}

	/**
     * Attempts to right-click-use an item by the given EntityPlayer
     */
    public static boolean tryUseItem(EntityPlayer entityPlayer, ItemStack itemStack, Side side)
    {
        if(side.isClient()){
        	TheOffhandMod.packetHandler.sendPacketToServer(new OffhandPlaceBlockPacket(-1, -1, -1, 255, itemStack, 0.0F, 0.0F, 0.0F).generatePacket());
        }
        final int i = itemStack.stackSize;
        final int j = itemStack.getItemDamage();
        ItemStack itemstack1 = itemStack.useItemRightClick(entityPlayer.getEntityWorld(), entityPlayer);

        if (itemstack1 == itemStack && (itemstack1 == null || itemstack1.stackSize == i && (side.isServer()?(itemstack1.getMaxItemUseDuration() <= 0 && itemstack1.getItemDamage() == j):true)))
        {
            return false;
        }
        else
        {
            BattlegearUtils.setPlayerOffhandItem(entityPlayer, itemstack1);
            if (side.isServer() && (entityPlayer).capabilities.isCreativeMode)
            {
                itemstack1.stackSize = i;
                if (itemstack1.isItemStackDamageable())
                {
                    itemstack1.setItemDamage(j);
                }
            }
            if (itemstack1.stackSize <= 0)
            {
                BattlegearUtils.setPlayerOffhandItem(entityPlayer, null);
                ForgeEventFactory.onPlayerDestroyItem(entityPlayer, itemstack1);
            }
            if (side.isServer() && !entityPlayer.isUsingItem())
            {
                ((EntityPlayerMP)entityPlayer).sendContainerToPlayer(entityPlayer.inventoryContainer);
            }
            return true;
        }
    }

    @SideOnly(Side.CLIENT)
    public static void sendOffSwingEvent(PlayerEvent event, ItemStack mainHandItem, ItemStack offhandItem){
        if(!MinecraftForge.EVENT_BUS.post(new PlayerEventChild.OffhandSwingEvent(event, mainHandItem, offhandItem))){
            ((IBattlePlayer) event.entityPlayer).swingOffItem();
            TheOffhandMod.proxy.sendAnimationPacket(EnumBGAnimations.OffHandSwing, event.entityPlayer);
        }
    }

    @SideOnly(Side.CLIENT)
    public static void sendOffSwingEventNoCheck(ItemStack mainHandItem, ItemStack offhandItem){
        ((IBattlePlayer) Minecraft.getMinecraft().thePlayer).swingOffItem();
        TheOffhandMod.proxy.sendAnimationPacket(EnumBGAnimations.OffHandSwing, Minecraft.getMinecraft().thePlayer);
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public void onOffhandSwing(PlayerEventChild.OffhandSwingEvent event){
        if(event.offHand != null && event.parent.getClass().equals(PlayerInteractEvent.class)){
            if (event.offHand.getItem() instanceof IShield || (!BattlegearUtils.reverseactionconfirmed && BattlegearUtils.usagePriorAttack(event.offHand))){
                event.setCanceled(true);
            }else if(event.offHand.getItem() instanceof IOffhandDual){
                boolean shouldSwing = true;
                if(((PlayerInteractEvent)event.parent).action == PlayerInteractEvent.Action.RIGHT_CLICK_AIR)
                    shouldSwing = ((IOffhandDual) event.offHand.getItem()).offhandClickAir((PlayerInteractEvent)event.parent, event.mainHand, event.offHand);
                else if(((PlayerInteractEvent)event.parent).action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK){
                    ((PlayerInteractEvent)event.parent).useItem = Event.Result.DENY;
                    shouldSwing = ((IOffhandDual) event.offHand.getItem()).offhandClickBlock((PlayerInteractEvent)event.parent, event.mainHand, event.offHand);
                }
                if(!shouldSwing){
                    event.setCanceled(true);
                }
            }
        }

        if(MysteriumPatchesFixesO.shouldNotOverride){
          event.setCanceled(true);
          event.setCancelParentEvent(false);
        }
//        if(event.mainHand !=null && BattlegearUtils.isBow(event.mainHand.getItem()) && event.parent.getClass().equals(PlayerInteractEvent.class)){
//            event.setCanceled(true);
//            event.setCancelParentEvent(false);
//        }
    }
    
    public static boolean dontdoit = false;
    
    public boolean interactWithNoEvent(EntityPlayer pl, Entity p_70998_1_)
    {
        ItemStack itemstack = pl.getCurrentEquippedItem();
        ItemStack itemstack1 = itemstack != null ? itemstack.copy() : null;

        if (!p_70998_1_.interactFirst(pl))
        {
            if (itemstack != null && p_70998_1_ instanceof EntityLivingBase)
            {
                if (pl.capabilities.isCreativeMode)
                {
                    itemstack = itemstack1;
                }

                if (itemstack.interactWithEntity(pl, (EntityLivingBase)p_70998_1_))
                {
                    if (itemstack.stackSize <= 0 && !pl.capabilities.isCreativeMode)
                    {
                        pl.destroyCurrentEquippedItem();
                    }

                    return true;
                }
            }

            return false;
        }
        else
        {
            if (itemstack != null && itemstack == pl.getCurrentEquippedItem())
            {
                if (itemstack.stackSize <= 0 && !pl.capabilities.isCreativeMode)
                {
                    pl.destroyCurrentEquippedItem();
                }
                else if (itemstack.stackSize < itemstack1.stackSize && pl.capabilities.isCreativeMode)
                {
                    itemstack.stackSize = itemstack1.stackSize;
                }
            }

            return true;
        }
    }
    
    public static boolean interactWith = false;

    @SubscribeEvent
    public void playerIntereactEntity(EntityInteractEvent event) {
        if(isFake(event.entityPlayer))
            return;
        if(((IBattlePlayer) event.entityPlayer).getSpecialActionTimer() > 0){
            event.setCanceled(true);
            event.setResult(Event.Result.DENY);
            event.entityPlayer.isSwingInProgress = false;
        } else if (((IBattlePlayer) event.entityPlayer).isBattlemode()) {
        	if (!BattlegearUtils.reverseactionconfirmed) {
        		interactWith = interactWithNoEvent(event.entityPlayer, event.target);
        	}
            ItemStack offhandItem = ((InventoryPlayerBattle)event.entityPlayer.inventory).getCurrentOffhandWeapon();
            if((offhandItem == null || !BattlegearUtils.usagePriorAttack(offhandItem) || BattlegearUtils.reverseactionconfirmed)){
                ItemStack mainHandItem = event.entityPlayer.getCurrentEquippedItem();
                PlayerEventChild.OffhandAttackEvent offAttackEvent = new PlayerEventChild.OffhandAttackEvent(event, mainHandItem, offhandItem);
                if (!MysteriumPatchesFixesO.shouldNotOverride) {
	                if(!MinecraftForge.EVENT_BUS.post(offAttackEvent)){
                    	if (interactWith) {
                        	interactWith = false;
                    		return;
                    	} else {
                    		event.setCanceled(true);
                    	}
	                    if (offAttackEvent.swingOffhand){
	                        if (event.entityPlayer.worldObj.isRemote) sendOffSwingEvent(event, mainHandItem, offhandItem);
	                    }
	                    if (offAttackEvent.shouldAttack)
	                    {
	                        ((IBattlePlayer) event.entityPlayer).attackTargetEntityWithCurrentOffItem(event.target);
	                    }
	                    if (offAttackEvent.cancelParent) {
	                        event.setCanceled(true);
	                    }
	                }
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onOffhandAttack(PlayerEventChild.OffhandAttackEvent event){
    	if(event.offHand != null){
            if(event.offHand.getItem() instanceof IOffhandDual){
                event.swingOffhand =((IOffhandDual) event.offHand.getItem()).offhandAttackEntity(event, event.mainHand, event.offHand);
            }else if(event.offHand.getItem() instanceof IShield){
                event.swingOffhand = false;
                event.shouldAttack = false;
            }else if(!BattlegearUtils.reverseactionconfirmed && hasEntityInteraction(event.getPlayer().capabilities.isCreativeMode?event.offHand.copy():event.offHand, event.getTarget(), event.getPlayer(), false)){
            	event.setCanceled(true);
                if(event.offHand.stackSize<=0 && !event.getPlayer().capabilities.isCreativeMode){
                    ItemStack orig = event.offHand;
                    BattlegearUtils.setPlayerOffhandItem(event.getPlayer(), null);
                    MinecraftForge.EVENT_BUS.post(new PlayerDestroyItemEvent(event.getPlayer(), orig));
                }
                return;
            }
        }
//        if(event.mainHand != null) {
//            if (BattlegearUtils.isBow(event.mainHand.getItem())) {
//                event.swingOffhand = false;
//                event.shouldAttack = false;
//            } else if (hasEntityInteraction(event.mainHand, event.getTarget(), event.getPlayer(), true)) {
//                event.setCanceled(true);
//                event.setCancelParentEvent(false);
//            }
//        }
    }

    /**
     * Check if a stack has a specific interaction with an entity.
     * Use a call to {@link net.minecraft.item.ItemStack#interactWithEntity(EntityPlayer, EntityLivingBase)}
     *
     * @param itemStack to interact last with
     * @param entity to interact first with
     * @param entityPlayer holding the stack
     * @param asTest if data should be cloned before testing
     * @return true if a specific interaction exist (and has been done if asTest is false)
     */
    private boolean hasEntityInteraction(ItemStack itemStack, Entity entity, EntityPlayer entityPlayer, boolean asTest){
        if (asTest) {
            Entity clone = EntityList.createEntityByName(EntityList.getEntityString(entity), entity.worldObj);
            if (clone != null) {
                clone.copyDataFrom(entity, true);
                return !clone.interactFirst(entityPlayer) && clone instanceof EntityLivingBase && itemStack.copy().interactWithEntity(entityPlayer, (EntityLivingBase) clone);
            }
        } else if(!entity.interactFirst(entityPlayer) && entity instanceof EntityLivingBase){
            return itemStack.interactWithEntity(entityPlayer, (EntityLivingBase) entity);
        }
        return false;
    }

    @SubscribeEvent
    public void shieldHook(LivingHurtEvent event){
        if(isFake(event.entity))
            return;
        if(event.entity instanceof IBattlePlayer){
            EntityPlayer player = (EntityPlayer)event.entity;
            if(((IBattlePlayer) player).getSpecialActionTimer() > 0){
                event.setCanceled(true);
            } else if(((IBattlePlayer) player).isBlockingWithShield()){
                final ItemStack shield = ((InventoryPlayerBattle)player.inventory).getCurrentOffhandWeapon();
                final float dmg = event.ammount;
                if(((IShield)shield.getItem()).canBlock(shield, event.source)){
                    boolean shouldBlock = true;
                    Entity opponent = event.source.getEntity();
                    if(opponent != null){
                        double d0 = opponent.posX - event.entity.posX;
                        double d1;

                        for (d1 = opponent.posZ - player.posZ; d0 * d0 + d1 * d1 < 1.0E-4D; d1 = (Math.random() - Math.random()) * 0.01D){
                            d0 = (Math.random() - Math.random()) * 0.01D;
                        }

                        float yaw = (float)(Math.atan2(d1, d0) * 180.0D / Math.PI) - player.rotationYaw;
                        yaw = yaw - 90;

                        while(yaw < -180){
                            yaw+= 360;
                        }
                        while(yaw >= 180){
                            yaw-=360;
                        }

                        float blockAngle = ((IShield) shield.getItem()).getBlockAngle(shield);

                        shouldBlock = yaw < blockAngle && yaw > -blockAngle;
                        //player.knockBack(opponent, 50, 100, 100);
                    }

                    if(shouldBlock){
                        PlayerEventChild.ShieldBlockEvent blockEvent = new PlayerEventChild.ShieldBlockEvent(new PlayerEvent(player), shield, event.source, dmg);
                        MinecraftForge.EVENT_BUS.post(blockEvent);
                        if (blockEvent.ammountRemaining > 0.0F) {
                            event.ammount = blockEvent.ammountRemaining;
                        } else {
                            event.setCanceled(true);
                        }

                        if(blockEvent.performAnimation){
                        	TheOffhandMod.packetHandler.sendPacketAround(player, 32, new BattlegearShieldFlashPacket(player, dmg).generatePacket());
                            ((IShield)shield.getItem()).blockAnimation(player, dmg);
                        }

                        if(event.source.isProjectile() && event.source.getSourceOfDamage() instanceof IProjectile){
                            if(shield.getItem() instanceof IArrowCatcher){
                                if(((IArrowCatcher)shield.getItem()).catchArrow(shield, player, (IProjectile)event.source.getSourceOfDamage())){
                                    ((InventoryPlayerBattle)player.inventory).hasChanged = true;
                                }
                            }
                        }

                        if(blockEvent.damageShield && !player.capabilities.isCreativeMode){
                            float red = ((IShield)shield.getItem()).getDamageReduction(shield, event.source);
                            if(red<dmg){
                                shield.damageItem(Math.round(dmg-red), player);
                                if(shield.stackSize <= 0){
                                    ForgeEventFactory.onPlayerDestroyItem(player, shield);
                                    player.inventory.setInventorySlotContents(player.inventory.currentItem + 3, null);
                                    //TODO Render item break
                                }
                                ((InventoryPlayerBattle)player.inventory).hasChanged = true;
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void addTracking(PlayerEvent.StartTracking event){
        if(event.target instanceof EntityPlayer && !isFake(event.target)){
            ((EntityPlayerMP)event.entityPlayer).playerNetServerHandler.sendPacket(new BattlegearSyncItemPacket((EntityPlayer) event.target).generatePacket());
        }
    }
}
