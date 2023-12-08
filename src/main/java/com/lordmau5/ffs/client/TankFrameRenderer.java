package com.lordmau5.ffs.client;

import com.lordmau5.ffs.tile.TileEntityTankFrame;
import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.MinecraftForgeClient;

public class TankFrameRenderer implements ISimpleBlockRenderingHandler {
   public static final int id = RenderingRegistry.getNextAvailableRenderId();

   public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
   }

   public static int getPassForFrameRender(RenderBlocks rb) {
      return MinecraftForgeClient.getRenderPass();
   }

   public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) {
      return this.renderWorldBlock(world, x, y, z, block, renderer, getPassForFrameRender(renderer));
   }

   public boolean renderWorldBlock(IBlockAccess ba, int x, int y, int z, Block block, RenderBlocks rb, int pass) {
      Tessellator.instance.addVertexWithUV((double)x, (double)y, (double)z, 0.0D, 0.0D);
      Tessellator.instance.addVertexWithUV((double)x, (double)y, (double)z, 0.0D, 0.0D);
      Tessellator.instance.addVertexWithUV((double)x, (double)y, (double)z, 0.0D, 0.0D);
      Tessellator.instance.addVertexWithUV((double)x, (double)y, (double)z, 0.0D, 0.0D);
      TileEntity tile = ba.getTileEntity(x, y, z);
      if (!(tile instanceof TileEntityTankFrame)) {
         return false;
      } else {
         boolean invalidRender = true;
         Block renderBlock = Blocks.stone;
         TileEntityTankFrame te = (TileEntityTankFrame)tile;
         if (te.getBlock() != null) {
            Block exBlock = te.getBlock().getBlock();
            if (exBlock != null) {
               renderBlock = exBlock;
               invalidRender = false;
            }
         }

         IBlockAccess origBa = rb.blockAccess;
         boolean isFrameBlockOpaque = renderBlock.isOpaqueCube();
         if ((isFrameBlockOpaque || renderBlock.canRenderInPass(0)) && pass == 0 || (!isFrameBlockOpaque || renderBlock.canRenderInPass(1)) && pass == 1) {
            if (invalidRender) {
               rb.renderStandardBlock(renderBlock, x, y, z);
            } else {
               rb.blockAccess = new FrameBlockAccessWrapper(origBa);

               try {
                  rb.renderBlockByRenderType(renderBlock, x, y, z);
               } catch (Exception var15) {
                  rb.renderStandardBlock(Blocks.stone, x, y, z);
               }
            }

            rb.blockAccess = origBa;
         }

         return true;
      }
   }

   public boolean shouldRender3DInInventory(int modelId) {
      return false;
   }

   public int getRenderId() {
      return id;
   }
}
