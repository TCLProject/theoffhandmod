package mods.battlegear2.gui;

import cpw.mods.fml.common.network.IGuiHandler;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.world.World;

public final class BattlegearGUIHandeler implements IGuiHandler {
    public static final int mainID = 0;
    public static final int sigilEditor = 2;
    public static final int flagEditor = 3;

    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world,
                                      int x, int y, int z) {
        switch (ID) {
            case mainID:
                return new ContainerPlayer(player.inventory, !world.isRemote, player);
            default:
                return null;
        }

    }

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world,
                                      int x, int y, int z) {
        switch (ID) {
            case mainID:
                return new GuiInventory(player);
            default:
                return null;
        }
    }

}
