package net.tclproject.theoffhandmod.packets;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import mods.battlegear2.api.core.InventoryPlayerBattle;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.tclproject.theoffhandmod.TheOffhandMod;

public class InactiveHandSyncClient implements IMessage {

	private NBTTagCompound data;

	 // The basic, no-argument constructor MUST be included to use the new automated handling
		public InactiveHandSyncClient() {}

	 // We need to initialize our data, so provide a suitable constructor:
		public InactiveHandSyncClient(EntityPlayer player) {
			data = new NBTTagCompound();
			data.setInteger("inactivehand", ((InventoryPlayerBattle)player.inventory).currentItemInactive);
		}

		@Override
	 	public void fromBytes(ByteBuf buffer) {
		 	data = ByteBufUtils.readTag(buffer);
	 	}

		 @Override
		 public void toBytes(ByteBuf buffer) {
			 ByteBufUtils.writeTag(buffer, data);
		 }

		 public static class Handler implements IMessageHandler<InactiveHandSyncClient, IMessage> {

			 @Override
		     public IMessage onMessage(InactiveHandSyncClient message, MessageContext ctx) {
				 if (message.data.getInteger("inactivehand") > 153 && message.data.getInteger("inactivehand") < 158) {
					 ((InventoryPlayerBattle)TheOffhandMod.proxy.getClientPlayer().inventory).currentItemInactive = message.data.getInteger("inactivehand");
				 } else {
					((InventoryPlayerBattle)TheOffhandMod.proxy.getClientPlayer().inventory).currentItemInactive = 154;
				 }
				 return null;
			 }
		 }
}
