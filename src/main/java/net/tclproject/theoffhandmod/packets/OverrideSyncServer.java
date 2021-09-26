package net.tclproject.theoffhandmod.packets;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import mods.battlegear2.api.core.BattlegearUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.tclproject.mysteriumlib.asm.fixes.MysteriumPatchesFixesO;

public class OverrideSyncServer implements IMessage {

	private NBTTagCompound data;

	 // The basic, no-argument constructor MUST be included to use the new automated handling
		public OverrideSyncServer() {}

	 // We need to initialize our data, so provide a suitable constructor:
		public OverrideSyncServer(EntityPlayer player) {
			data = new NBTTagCompound();
			data.setBoolean("override", MysteriumPatchesFixesO.shouldNotOverride);
			data.setBoolean("rightconfirm", BattlegearUtils.rightclickconfirmed);
		}

		@Override
	 	public void fromBytes(ByteBuf buffer) {
		 	data = ByteBufUtils.readTag(buffer);
	 	}

		 @Override
		 public void toBytes(ByteBuf buffer) {
			 ByteBufUtils.writeTag(buffer, data);
		 }

		 public static class Handler implements IMessageHandler<OverrideSyncServer, IMessage> {

			 @Override
		     public IMessage onMessage(OverrideSyncServer message, MessageContext ctx) {
					MysteriumPatchesFixesO.shouldNotOverride = message.data.getBoolean("override");
					BattlegearUtils.rightclickconfirmed = message.data.getBoolean("rightconfirm");
					return null;
			 }
		 }
}
