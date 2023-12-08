package com.lordmau5.ffs.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.ForgeDirection;

public class IBlockAccessHandler implements IBlockAccess {
   protected final IBlockAccess ba;

   public IBlockAccessHandler(IBlockAccess iBlockAccess) {
      this.ba = iBlockAccess;
   }

   public boolean isSideSolid(int x, int y, int z, ForgeDirection side, boolean _default) {
      return this.ba.isSideSolid(x, y, z, side, _default);
   }

   public int isBlockProvidingPowerTo(int x, int y, int z, int dir) {
      return this.ba.isBlockProvidingPowerTo(x, y, z, dir);
   }

   public boolean isAirBlock(int x, int y, int z) {
      return this.ba.isAirBlock(x, y, z);
   }

   public TileEntity getTileEntity(int x, int y, int z) {
      return y >= 0 && y < 256 ? this.ba.getTileEntity(x, y, z) : null;
   }

   @SideOnly(Side.CLIENT)
   public int getLightBrightnessForSkyBlocks(int x, int y, int z, int var_) {
      return 15728880;
   }

   @SideOnly(Side.CLIENT)
   public int getHeight() {
      return this.ba.getHeight();
   }

   public int getBlockMetadata(int x, int y, int z) {
      return this.ba.getBlockMetadata(x, y, z);
   }

   public Block getBlock(int x, int y, int z) {
      return this.ba.getBlock(x, y, z);
   }

   @SideOnly(Side.CLIENT)
   public BiomeGenBase getBiomeGenForCoords(int cX, int cZ) {
      return this.ba.getBiomeGenForCoords(cX, cZ);
   }

   @SideOnly(Side.CLIENT)
   public boolean extendedLevelsInChunkCache() {
      return this.ba.extendedLevelsInChunkCache();
   }
}
