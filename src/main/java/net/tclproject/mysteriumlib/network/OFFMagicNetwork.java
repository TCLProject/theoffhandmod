package net.tclproject.mysteriumlib.network;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import net.tclproject.theoffhandmod.packets.*;

public final class OFFMagicNetwork {

	public static final SimpleNetworkWrapper dispatcher = NetworkRegistry.INSTANCE.newSimpleChannel("theoffhandmod");

	public static final void registerPackets() {
       	// Registration
		dispatcher.registerMessage(OverrideSyncServer.Handler.class, OverrideSyncServer.class, 0, Side.SERVER);
		dispatcher.registerMessage(InactiveHandSyncServer.Handler.class, InactiveHandSyncServer.class, 1, Side.SERVER);
		dispatcher.registerMessage(InactiveHandSyncClient.Handler.class, InactiveHandSyncClient.class, 2, Side.CLIENT);
    }

}
