package com.lordmau5.ffs.blocks;

import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.client.ValveRenderer;
import com.lordmau5.ffs.tile.TileEntityValve;
import com.lordmau5.ffs.util.GenericUtil;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.common.util.ForgeDirection;

public class BlockValve extends Block {
   public BlockValve() {
      super(Material.iron);
      this.setBlockName("blockValve");
      this.setBlockTextureName("FFS:blockValve");
      this.setCreativeTab(CreativeTabs.tabRedstone);
      this.setHardness(5.0F);
      this.setResistance(10.0F);
   }

   public boolean hasTileEntity(int metadata) {
      return true;
   }

   public TileEntity createTileEntity(World world, int metadata) {
      return new TileEntityValve();
   }

   public void registerBlockIcons(IIconRegister iR) {
      super.registerBlockIcons(iR);
      FancyFluidStorage.proxy.registerIcons(iR);
   }

   public void onBlockExploded(World world, int x, int y, int z, Explosion explosion) {
      TileEntity tile = world.getTileEntity(x, y, z);
      if (tile != null && tile instanceof TileEntityValve) {
         TileEntityValve valve = (TileEntityValve)world.getTileEntity(x, y, z);
         valve.breakTank((TileEntity)null);
      }

      super.onBlockDestroyedByExplosion(world, x, y, z, explosion);
   }

   public void breakBlock(World world, int x, int y, int z, Block block, int metadata) {
      if (!world.isRemote) {
         TileEntityValve valve = (TileEntityValve)world.getTileEntity(x, y, z);
         valve.breakTank((TileEntity)null);
      }

      super.breakBlock(world, x, y, z, block, metadata);
   }

   public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
      if (super.onBlockActivated(world, x, y, z, player, side, hitX, hitY, hitZ)) {
         return true;
      } else if (player.isSneaking()) {
         return false;
      } else {
         TileEntityValve valve = (TileEntityValve)world.getTileEntity(x, y, z);
         if (valve.isValid()) {
            if (GenericUtil.isFluidContainer(player.getHeldItem())) {
               return GenericUtil.fluidContainerHandler(world, x, y, z, valve, player);
            } else {
               player.openGui(FancyFluidStorage.instance, 0, world, x, y, z);
               return true;
            }
         } else {
            valve.buildTank(ForgeDirection.getOrientation(side).getOpposite());
            return true;
         }
      }
   }

   public Item getItemDropped(int p_149650_1_, Random p_149650_2_, int p_149650_3_) {
      return Item.getItemFromBlock(FancyFluidStorage.blockValve);
   }

   @SideOnly(Side.CLIENT)
   public int getRenderType() {
      return ValveRenderer.id;
   }

   public boolean canRenderInPass(int pass) {
      ForgeHooksClient.setRenderPass(pass);
      return true;
   }

   public int getRenderBlockPass() {
      return 0;
   }

   public boolean renderAsNormalBlock() {
      return false;
   }

   public boolean isOpaqueCube() {
      return false;
   }

   public boolean isNormalCube(IBlockAccess world, int x, int y, int z) {
      return true;
   }

   public boolean isNormalCube() {
      return true;
   }

   public boolean hasComparatorInputOverride() {
      return true;
   }

   public int getComparatorInputOverride(World world, int x, int y, int z, int side) {
      TileEntity te = world.getTileEntity(x, y, z);
      if (te instanceof TileEntityValve) {
         TileEntityValve valve = (TileEntityValve)te;
         return valve.getComparatorOutput();
      } else {
         return 0;
      }
   }

   public boolean canCreatureSpawn(EnumCreatureType type, IBlockAccess world, int x, int y, int z) {
      return false;
   }
}
