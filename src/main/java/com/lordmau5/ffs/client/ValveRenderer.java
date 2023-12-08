package com.lordmau5.ffs.client;

import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.tile.TileEntityValve;
import com.lordmau5.ffs.util.Position3D;
import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import org.lwjgl.opengl.GL11;

public class ValveRenderer extends TileEntitySpecialRenderer implements ISimpleBlockRenderingHandler {
   public static final int id = RenderingRegistry.getNextAvailableRenderId();

   private void preGL() {
      GL11.glPushMatrix();
      GL11.glPushAttrib(8192);
      GL11.glEnable(2884);
      GL11.glDisable(2896);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      this.bindTexture(TextureMap.locationBlocksTexture);
   }

   private void postGL() {
      GL11.glPopAttrib();
      GL11.glPopMatrix();
   }

   public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float f) {
      TileEntityValve valve = (TileEntityValve)tile;
      if (valve != null && valve.isValid()) {
         Tessellator t = Tessellator.instance;
         if (valve.isMaster()) {
            Position3D bottomDiag = valve.bottomDiagFrame;
            Position3D topDiag = valve.topDiagFrame;
            if (bottomDiag == null || topDiag == null) {
               return;
            }

            int height = topDiag.getY() - bottomDiag.getY();
            int xSize = topDiag.getX() - bottomDiag.getX() + (FancyFluidStorage.instance.TANK_RENDER_INSIDE ? 0 : 1);
            int zSize = topDiag.getZ() - bottomDiag.getZ() + (FancyFluidStorage.instance.TANK_RENDER_INSIDE ? 0 : 1);
            if (valve.getCapacity() == 0 || valve.getFluidAmount() == 0) {
               return;
            }

            double fillPercentage = (double)valve.getFluidAmount() / (double)valve.getCapacity();
            if (fillPercentage > 0.0D && valve.getFluid() != null) {
               FluidStack fluid = valve.getFluid();
               this.preGL();
               IIcon flowing = FluidHelper.getFluidTexture(fluid.getFluid(), true);
               IIcon still = FluidHelper.getFluidTexture(fluid.getFluid(), false);
               float stillMinU = still.getMinU();
               float stillMaxU = still.getMaxU();
               float stillMinV = still.getMinV();
               float stillMaxV = still.getMaxV();
               float flowMinU = flowing.getMinU();
               float flowMaxU = flowMinU + (stillMaxU - stillMinU);
               float flowMinV_ = flowing.getMinV();
               float flowMaxV_ = flowMinV_ + (stillMaxV - stillMinV);
               GL11.glTranslatef((float)x, (float)y, (float)z);
               GL11.glTranslatef((float)(bottomDiag.getX() - tile.xCoord), (float)bottomDiag.getY() - (float)tile.yCoord + 1.0F, (float)(bottomDiag.getZ() - tile.zCoord));
               t.startDrawingQuads();
               float pureRenderHeight = (float)(height - 1) * (float)fillPercentage;
               boolean isNegativeDensity = fluid.getFluid().getDensity(fluid) < 0;

               for(int rY = 0; (double)rY < (isNegativeDensity ? (double)(height - 1) : Math.ceil((double)pureRenderHeight)); ++rY) {
                  float renderHeight = pureRenderHeight - (float)rY;
                  renderHeight = Math.min(renderHeight, 1.0F) + (float)rY;
                  if (rY == 0) {
                     renderHeight = Math.max(0.01F, renderHeight);
                  }

                  if (isNegativeDensity) {
                     renderHeight = 1.0F + (float)rY;
                  }

                  float flowMinV = flowMinV_;
                  if (renderHeight - (float)rY < 1.0F) {
                     flowMinV = flowMinV_ + (flowMaxV_ - flowMinV_) * (1.0F - (renderHeight - (float)rY));
                  }

                  for(int rX = FancyFluidStorage.instance.TANK_RENDER_INSIDE ? 1 : 0; rX < xSize; ++rX) {
                     for(int rZ = FancyFluidStorage.instance.TANK_RENDER_INSIDE ? 1 : 0; rZ < zSize; ++rZ) {
                        float zMinOffset = 0.0F;
                        float zMaxOffset = 0.0F;
                        float xMinOffset = 0.0F;
                        float xMaxOffset = 0.0F;
                        if (rZ == (FancyFluidStorage.instance.TANK_RENDER_INSIDE ? 1 : 0)) {
                           zMinOffset = 0.005F;
                           t.addVertexWithUV((double)rX, (double)rY, (double)((float)rZ + zMinOffset), (double)flowMaxU, (double)flowMaxV_);
                           t.addVertexWithUV((double)rX, (double)renderHeight, (double)((float)rZ + zMinOffset), (double)flowMaxU, (double)flowMinV);
                           t.addVertexWithUV((double)(rX + 1), (double)renderHeight, (double)((float)rZ + zMinOffset), (double)flowMinU, (double)flowMinV);
                           t.addVertexWithUV((double)(rX + 1), (double)rY, (double)((float)rZ + zMinOffset), (double)flowMinU, (double)flowMaxV_);
                        }

                        if (rZ == zSize - 1) {
                           zMaxOffset = 0.005F;
                           t.addVertexWithUV((double)rX, (double)rY, (double)((float)(rZ + 1) - zMaxOffset), (double)flowMaxU, (double)flowMaxV_);
                           t.addVertexWithUV((double)(rX + 1), (double)rY, (double)((float)(rZ + 1) - zMaxOffset), (double)flowMinU, (double)flowMaxV_);
                           t.addVertexWithUV((double)(rX + 1), (double)renderHeight, (double)((float)(rZ + 1) - zMaxOffset), (double)flowMinU, (double)flowMinV);
                           t.addVertexWithUV((double)rX, (double)renderHeight, (double)((float)(rZ + 1) - zMaxOffset), (double)flowMaxU, (double)flowMinV);
                        }

                        if (rX == (FancyFluidStorage.instance.TANK_RENDER_INSIDE ? 1 : 0)) {
                           xMinOffset = 0.005F;
                           t.addVertexWithUV((double)((float)rX + xMinOffset), (double)rY, (double)rZ, (double)flowMaxU, (double)flowMaxV_);
                           t.addVertexWithUV((double)((float)rX + xMinOffset), (double)rY, (double)(rZ + 1), (double)flowMinU, (double)flowMaxV_);
                           t.addVertexWithUV((double)((float)rX + xMinOffset), (double)renderHeight, (double)(rZ + 1), (double)flowMinU, (double)flowMinV);
                           t.addVertexWithUV((double)((float)rX + xMinOffset), (double)renderHeight, (double)rZ, (double)flowMaxU, (double)flowMinV);
                        }

                        if (rX == xSize - 1) {
                           xMaxOffset = 0.005F;
                           t.addVertexWithUV((double)((float)(rX + 1) - xMaxOffset), (double)rY, (double)rZ, (double)flowMaxU, (double)flowMaxV_);
                           t.addVertexWithUV((double)((float)(rX + 1) - xMaxOffset), (double)renderHeight, (double)rZ, (double)flowMaxU, (double)flowMinV);
                           t.addVertexWithUV((double)((float)(rX + 1) - xMaxOffset), (double)renderHeight, (double)(rZ + 1), (double)flowMinU, (double)flowMinV);
                           t.addVertexWithUV((double)((float)(rX + 1) - xMaxOffset), (double)rY, (double)(rZ + 1), (double)flowMinU, (double)flowMaxV_);
                        }

                        if (isNegativeDensity) {
                           if (rY == height - 2) {
                              t.addVertexWithUV((double)((float)rX + xMinOffset), (double)renderHeight, (double)rZ, (double)stillMinU, (double)stillMinV);
                              t.addVertexWithUV((double)((float)rX + xMinOffset), (double)renderHeight, (double)(rZ + 1), (double)stillMinU, (double)stillMaxV);
                              t.addVertexWithUV((double)((float)(rX + 1) - xMaxOffset), (double)renderHeight, (double)(rZ + 1), (double)stillMaxU, (double)stillMaxV);
                              t.addVertexWithUV((double)((float)(rX + 1) - xMaxOffset), (double)renderHeight, (double)rZ, (double)stillMaxU, (double)stillMinV);
                           }
                        } else if ((double)rY == Math.floor((double)pureRenderHeight) || (double)(rY + 1) == Math.ceil((double)pureRenderHeight)) {
                           t.addVertexWithUV((double)((float)rX + xMinOffset), (double)renderHeight, (double)rZ, (double)stillMinU, (double)stillMinV);
                           t.addVertexWithUV((double)((float)rX + xMinOffset), (double)renderHeight, (double)(rZ + 1), (double)stillMinU, (double)stillMaxV);
                           t.addVertexWithUV((double)((float)(rX + 1) - xMaxOffset), (double)renderHeight, (double)(rZ + 1), (double)stillMaxU, (double)stillMaxV);
                           t.addVertexWithUV((double)((float)(rX + 1) - xMaxOffset), (double)renderHeight, (double)rZ, (double)stillMaxU, (double)stillMinV);
                        }

                        t.addVertexWithUV((double)(rX + 1), 0.009999999776482582D, (double)((float)rZ + zMinOffset), (double)stillMinU, (double)stillMinV);
                        t.addVertexWithUV((double)(rX + 1), 0.009999999776482582D, (double)((float)(rZ + 1) - zMaxOffset), (double)stillMinU, (double)stillMaxV);
                        t.addVertexWithUV((double)rX, 0.009999999776482582D, (double)((float)(rZ + 1) - zMaxOffset), (double)stillMaxU, (double)stillMaxV);
                        t.addVertexWithUV((double)rX, 0.009999999776482582D, (double)((float)rZ + zMinOffset), (double)stillMaxU, (double)stillMinV);
                     }
                  }
               }

               if (isNegativeDensity) {
                  GL11.glColor4d(1.0D, 1.0D, 1.0D, 0.125D + fillPercentage - 0.125D * fillPercentage);
                  GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);
                  GL11.glTranslatef((float)(bottomDiag.getX() - topDiag.getX() - 1), (float)(-height + 1), 0.0F);
               }

               t.draw();
               this.postGL();
            }
         }

      }
   }

   public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
      Tessellator tessellator = Tessellator.instance;
      GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
      tessellator.startDrawingQuads();
      tessellator.setNormal(0.0F, -1.0F, 0.0F);
      renderer.renderFaceYNeg(block, 0.0D, 0.0D, 0.0D, renderer.getIconSafe(renderer.getIconSafe(FancyFluidStorage.proxy.tex_ValveItem)));
      tessellator.draw();
      tessellator.startDrawingQuads();
      tessellator.setNormal(0.0F, 1.0F, 0.0F);
      renderer.renderFaceYPos(block, 0.0D, 0.0D, 0.0D, renderer.getIconSafe(FancyFluidStorage.proxy.tex_ValveItem));
      tessellator.draw();
      tessellator.startDrawingQuads();
      tessellator.setNormal(0.0F, 0.0F, -1.0F);
      renderer.renderFaceZNeg(block, 0.0D, 0.0D, 0.0D, renderer.getIconSafe(FancyFluidStorage.proxy.tex_ValveItem));
      tessellator.draw();
      tessellator.startDrawingQuads();
      tessellator.setNormal(0.0F, 0.0F, 1.0F);
      renderer.renderFaceZPos(block, 0.0D, 0.0D, 0.0D, renderer.getIconSafe(FancyFluidStorage.proxy.tex_ValveItem));
      tessellator.draw();
      tessellator.startDrawingQuads();
      tessellator.setNormal(-1.0F, 0.0F, 0.0F);
      renderer.renderFaceXNeg(block, 0.0D, 0.0D, 0.0D, renderer.getIconSafe(FancyFluidStorage.proxy.tex_ValveItem));
      tessellator.draw();
      tessellator.startDrawingQuads();
      tessellator.setNormal(1.0F, 0.0F, 0.0F);
      renderer.renderFaceXPos(block, 0.0D, 0.0D, 0.0D, renderer.getIconSafe(FancyFluidStorage.proxy.tex_ValveItem));
      tessellator.draw();
      GL11.glTranslatef(0.5F, 0.5F, 0.5F);
   }

   public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) {
      Tessellator.instance.addVertexWithUV((double)x, (double)y, (double)z, 0.0D, 0.0D);
      Tessellator.instance.addVertexWithUV((double)x, (double)y, (double)z, 0.0D, 0.0D);
      Tessellator.instance.addVertexWithUV((double)x, (double)y, (double)z, 0.0D, 0.0D);
      Tessellator.instance.addVertexWithUV((double)x, (double)y, (double)z, 0.0D, 0.0D);
      if (MinecraftForgeClient.getRenderPass() == 1) {
         return false;
      } else if (world == null) {
         return false;
      } else {
         TileEntity tile = world.getTileEntity(x, y, z);
         if (!(tile instanceof TileEntityValve)) {
            return false;
         } else {
            TileEntityValve valve = (TileEntityValve)tile;
            boolean isMaster = valve.isMaster();
            if (!valve.isValid()) {
               renderer.renderStandardBlock(block, x, y, z);
               renderer.setOverrideBlockTexture(isMaster ? FancyFluidStorage.proxy.tex_MasterValve[0] : FancyFluidStorage.proxy.tex_SlaveValve[0]);
               renderer.renderStandardBlock(block, x, y, z);
               renderer.clearOverrideBlockTexture();
               return false;
            } else {
               renderer.renderStandardBlock(block, x, y, z);
               ForgeDirection dr = valve.getInside();
               if (dr.offsetX != 0) {
                  renderer.renderFaceXNeg(block, (double)x, (double)y, (double)z, isMaster ? FancyFluidStorage.proxy.tex_MasterValve[1] : FancyFluidStorage.proxy.tex_SlaveValve[1]);
                  renderer.renderFaceXPos(block, (double)x, (double)y, (double)z, isMaster ? FancyFluidStorage.proxy.tex_MasterValve[1] : FancyFluidStorage.proxy.tex_SlaveValve[1]);
               } else if (dr.offsetY != 0) {
                  renderer.renderFaceYNeg(block, (double)x, (double)y, (double)z, isMaster ? FancyFluidStorage.proxy.tex_MasterValve[1] : FancyFluidStorage.proxy.tex_SlaveValve[1]);
                  renderer.renderFaceYPos(block, (double)x, (double)y, (double)z, isMaster ? FancyFluidStorage.proxy.tex_MasterValve[1] : FancyFluidStorage.proxy.tex_SlaveValve[1]);
               } else if (dr.offsetZ != 0) {
                  renderer.renderFaceZNeg(block, (double)x, (double)y, (double)z, isMaster ? FancyFluidStorage.proxy.tex_MasterValve[1] : FancyFluidStorage.proxy.tex_SlaveValve[1]);
                  renderer.renderFaceZPos(block, (double)x, (double)y, (double)z, isMaster ? FancyFluidStorage.proxy.tex_MasterValve[1] : FancyFluidStorage.proxy.tex_SlaveValve[1]);
               }

               return true;
            }
         }
      }
   }

   public boolean shouldRender3DInInventory(int modelId) {
      return true;
   }

   public int getRenderId() {
      return id;
   }
}
