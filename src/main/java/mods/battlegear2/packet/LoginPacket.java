package mods.battlegear2.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.tclproject.theoffhandmod.TheOffhandMod;

public final class LoginPacket extends AbstractMBPacket{
    public static final String packetName = "MB|Login";

    @Override
    public void process(ByteBuf inputStream, EntityPlayer player) {
        if(player.worldObj.isRemote){
        	TheOffhandMod.battlegearEnabled = true;
        }
    }

    public LoginPacket() {
    }

	@Override
	public String getChannel() {
		return packetName;
	}

	@Override
	public void write(ByteBuf out) {
		out.writeBytes(new byte[0]);
	}
}
