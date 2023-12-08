package com.lordmau5.ffs.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;

public final class FluidHelper {
   public static final ResourceLocation BLOCK_TEXTURE;

   public static IIcon getFluidTexture(Fluid fluid, boolean flowing) {
      if (fluid == null) {
         return null;
      } else {
         IIcon icon = flowing ? fluid.getFlowingIcon() : fluid.getStillIcon();
         if (icon == null) {
            icon = ((TextureMap)Minecraft.getMinecraft().getTextureManager().getTexture(TextureMap.locationBlocksTexture)).getAtlasSprite("missingno");
         }

         return (IIcon)icon;
      }
   }

   static {
      BLOCK_TEXTURE = TextureMap.locationBlocksTexture;
   }
}
