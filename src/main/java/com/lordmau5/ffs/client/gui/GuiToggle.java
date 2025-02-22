package com.lordmau5.ffs.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class GuiToggle extends GuiButton {
   protected static final ResourceLocation toggleTexture = new ResourceLocation("FFS:textures/gui/gui_tank.png");
   protected boolean state = false;
   protected int textColor = 16777215;

   public GuiToggle(int id, int x, int y, String title, boolean state, int textColor) {
      super(id, x, y, 16, 8, title);
      this.state = state;
      this.textColor = textColor;
   }

   public void drawButton(Minecraft mc, int mouseX, int mouseY) {
      if (this.visible) {
         FontRenderer fontrenderer = mc.fontRenderer;
         mc.getTextureManager().bindTexture(toggleTexture);
         GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
         GL11.glEnable(3042);
         OpenGlHelper.glBlendFunc(770, 771, 1, 0);
         GL11.glBlendFunc(770, 771);
         if (!this.state) {
            this.drawTexturedModalRect(this.xPosition, this.yPosition, 0, 121, this.width, this.height);
         } else {
            this.drawTexturedModalRect(this.xPosition, this.yPosition, 0, 129, this.width, this.height);
         }

         this.mouseDragged(mc, mouseX, mouseY);
         this.drawString(fontrenderer, this.displayString, this.xPosition + this.width + 4, this.yPosition, this.textColor);
      }

   }

   public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
      boolean onMe = super.mousePressed(mc, mouseX, mouseY);
      if (onMe) {
         this.state = !this.state;
      }

      return onMe;
   }

   public boolean getState() {
      return this.state;
   }

   public void setState(boolean state) {
      this.state = state;
   }
}
