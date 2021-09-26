package mods.battlegear2.client.gui;

import cpw.mods.fml.client.FMLClientHandler;
import mods.battlegear2.client.gui.controls.GuiDrawButton;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;

/**
 * A gui that displays like the in-game screen, where each element is a {@link GuiDrawButton}
 * Used to move gui elements and save their position into configuration file
 */
public final class BattlegearFakeGUI extends GuiScreen{
    private final GuiScreen previous;
    private final BattlegearInGameGUI helper = new BattlegearInGameGUI();
    public BattlegearFakeGUI(GuiScreen parent){
        this.previous = parent;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initGui(){
        this.buttonList.clear();
        this.buttonList.add(new GuiButton(1, this.width / 2 - 100, this.height - 25, I18n.format("gui.done")));
        this.buttonList.add(new GuiDrawButton(3, this.width / 2 - 91, this.height - 35, 182, 9, new BlockBarRenderer()));
        this.buttonList.add(new GuiDrawButton(4, this.width / 2 - 184, this.height - 22, 62, 22, new WeaponSlotRenderer(false)));
        this.buttonList.add(new GuiDrawButton(5, this.width / 2 + 121, this.height - 22, 62, 22, new WeaponSlotRenderer(true)));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float frame){
        this.drawGradientRect(0, 0, this.width, this.height, -1072689136, -804253680);
        super.drawScreen(mouseX, mouseY, frame);
        for(Object obj:this.buttonList)
            if(((GuiButton)obj).func_146115_a()){
                drawCreativeTabHoveringText(I18n.format("gui.fake.help"+((GuiButton) obj).id), mouseX, mouseY);
            }
    }

    @Override
    protected void actionPerformed(GuiButton button){
        if (button.enabled && button.id == 1){
            FMLClientHandler.instance().showGuiScreen(previous);
        }
    }

    @Override
    public void onGuiClosed(){
    }

    public final class WeaponSlotRenderer implements GuiDrawButton.IDrawnHandler{
        private final boolean isMainHand;
        public WeaponSlotRenderer(boolean isMainHand){
            this.isMainHand = isMainHand;
        }

        @Override
        public void drawElement(ScaledResolution resolution, int varX, int varY) {
            helper.renderBattleSlots(varX, varY, 0, isMainHand);
        }
    }

    public final class BlockBarRenderer implements GuiDrawButton.IDrawnHandler{
        ItemStack dummy;
        public BlockBarRenderer(){
        }
        @Override
        public void drawElement(ScaledResolution resolution, int varX, int varY) {
            if(dummy!=null){
                helper.renderBlockBar(varX, varY);
            }
        }
    }
}
