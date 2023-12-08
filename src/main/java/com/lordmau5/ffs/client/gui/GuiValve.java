package com.lordmau5.ffs.client.gui;

import com.lordmau5.ffs.client.FluidHelper;
import com.lordmau5.ffs.network.NetworkHandler;
import com.lordmau5.ffs.network.ffsPacket;
import com.lordmau5.ffs.tile.TileEntityValve;
import com.lordmau5.ffs.util.GenericUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class GuiValve extends GuiScreen {
   protected static final ResourceLocation tex = new ResourceLocation("FFS:textures/gui/gui_tank.png");
   protected static final int AUTO_FLUID_OUTPUT_BTN_ID = 23442;
   TileEntityValve valve;
   boolean isFrame;
   GuiTextField valveName;
   int xSize = 256;
   int ySize = 121;
   int left = 0;
   int top = 0;
   int mouseX;
   int mouseY;

   public GuiValve(TileEntityValve valve, boolean isFrame) {
      this.valve = valve;
      this.isFrame = isFrame;
   }

   public void initGui() {
      super.initGui();
      this.left = (this.width - this.xSize) / 2;
      this.top = (this.height - this.ySize) / 2;
      if (!this.isFrame) {
         this.buttonList.add(new GuiToggle(23442, this.left + 80, this.top + 20, "Auto fluid output", this.valve.getAutoOutput(), 16777215));
         this.valveName = new GuiTextField(this.fontRendererObj, this.left + 80, this.top + 100, 120, 10);
         this.valveName.setText(this.valve.getValveName());
         this.valveName.setMaxStringLength(18);
      }

   }

   public void onGuiClosed() {
      super.onGuiClosed();
      if (!this.isFrame && !this.valveName.getText().isEmpty()) {
         NetworkHandler.sendPacketToServer(new ffsPacket.Server.UpdateValveName(this.valve, this.valveName.getText()));
      }

   }

   protected void keyTyped(char keyChar, int keyCode) {
      if (!this.isFrame && this.valveName.isFocused()) {
         this.valveName.textboxKeyTyped(keyChar, keyCode);
      } else {
         if (keyCode == 1 || keyCode == this.mc.gameSettings.keyBindInventory.getKeyCode()) {
            this.mc.thePlayer.closeScreen();
            this.mc.setIngameFocus();
         }

      }
   }

   protected void mouseClicked(int p_73864_1_, int p_73864_2_, int p_73864_3_) {
      super.mouseClicked(p_73864_1_, p_73864_2_, p_73864_3_);
      if (!this.isFrame) {
         this.valveName.mouseClicked(p_73864_1_, p_73864_2_, p_73864_3_);
      }

   }

   public boolean doesGuiPauseGame() {
      return false;
   }

   public void drawScreen(int x, int y, float partialTicks) {
      this.mouseX = x;
      this.mouseY = y;
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      this.mc.renderEngine.bindTexture(tex);
      this.drawTexturedModalRect(this.left, this.top, 0, 0, this.xSize, this.ySize);
      String fluid = "Empty";
      if (this.valve.getFluid() != null) {
         fluid = this.valve.getFluid().getLocalizedName();
      }

      this.drawCenteredString(this.fontRendererObj, fluid + " Tank", this.left + 163, this.top + 6, 16777215);
      if (this.valve.getFluid() != null) {
         this.drawFluid(this.left, this.top);
      }

      super.drawScreen(x, y, partialTicks);
      if (!this.isFrame) {
         this.drawValveName(x, y);
      }

      if (this.valve.getFluid() != null) {
         this.fluidHoveringText(fluid);
      }

   }

   private void drawValveName(int x, int y) {
      this.drawString(this.fontRendererObj, "Valve Name:", this.left + 80, this.top + 88, 16777215);
      this.valveName.drawTextBox();
   }

   private void fluidHoveringText(String fluid) {
      if (this.mouseX >= this.left + 10 && this.mouseX < this.left + 10 + 64 && this.mouseY >= this.top + 10 && this.mouseY < this.top + 10 + 101) {
         List texts = new ArrayList();
         texts.add(fluid);
         texts.add("§" + EnumChatFormatting.GRAY.getFormattingCode() + GenericUtil.intToFancyNumber(this.valve.getFluidAmount()) + " / " + GenericUtil.intToFancyNumber(this.valve.getCapacity()) + " mB");
         GL11.glPushMatrix();
         GL11.glPushAttrib(64);
         this.drawHoveringText(texts, this.mouseX, this.mouseY, this.fontRendererObj);
         GL11.glPopAttrib();
         GL11.glPopMatrix();
      }

   }

   public void actionPerformed(GuiButton btn) {
      if (btn.id == 23442 && btn instanceof GuiToggle) {
         GuiToggle toggle = (GuiToggle)btn;
         this.valve.setAutoOutput(toggle.getState());
         NetworkHandler.sendPacketToServer(new ffsPacket.Server.UpdateAutoOutput(this.valve, this.valve.getAutoOutput()));
      }

   }

   private void drawFluid(int x, int y) {
      IIcon fluidIcon = FluidHelper.getFluidTexture(this.valve.getFluid().getFluid(), false);
      if (fluidIcon != null) {
         this.mc.getTextureManager().bindTexture(FluidHelper.BLOCK_TEXTURE);
         int height = (int)Math.ceil((double)((float)this.valve.getFluidAmount() / (float)this.valve.getCapacity() * 101.0F));
         ScaledResolution r = new ScaledResolution(this.mc, this.mc.displayWidth, this.mc.displayHeight);
         int sc = r.getScaleFactor();
         GL11.glScissor((x + 10) * sc, (y + 10 - this.mc.gameSettings.guiScale) * sc - sc, 64 * sc, (height + this.mc.gameSettings.guiScale + 1) * sc + sc % 2 - 1);
         GL11.glEnable(3089);

         for(int iX = 0; iX < 4; ++iX) {
            for(int iY = 7; iY > 0; --iY) {
               this.drawTexturedModelRectFromIcon(x + 10 + iX * 16, y - 1 + (iY - 1) * 16, fluidIcon, 16, 16);
            }
         }

         GL11.glDisable(3089);
      }
   }
}
