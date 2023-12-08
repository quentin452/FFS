package com.lordmau5.ffs.util;

import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.tile.TileEntityTankFrame;
import com.lordmau5.ffs.tile.TileEntityValve;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockGlass;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;
import net.minecraftforge.oredict.OreDictionary;

public class GenericUtil {
   private static List blacklistedBlocks;
   private static List validTiles;
   private static List glassList;

   public static void init() {
      glassList = OreDictionary.getOres("blockGlass");
      blacklistedBlocks = new ArrayList();
      blacklistedBlocks.add(Blocks.grass);
      blacklistedBlocks.add(Blocks.dirt);
      blacklistedBlocks.add(Blocks.bedrock);
      blacklistedBlocks.add(Blocks.redstone_lamp);
      blacklistedBlocks.add(Blocks.sponge);
      validTiles = new ArrayList();
      validTiles.add("blockFusedQuartz");
   }

   public static String getUniqueValveName(TileEntityValve valve) {
      return "valve_" + Integer.toHexString((new Position3D(valve.xCoord, valve.yCoord, valve.zCoord)).hashCode());
   }

   public static boolean canAutoOutput(float height, int tankHeight, int valvePosition, boolean negativeDensity) {
      height *= (float)tankHeight;
      if (negativeDensity) {
         return false;
      } else {
         return height > (float)valvePosition - 0.5F;
      }
   }

   public static boolean isBlockGlass(Block block, int metadata) {
      if (block != null && block.getMaterial() != Material.air) {
         if (block instanceof BlockGlass) {
            return true;
         } else {
            ItemStack is = new ItemStack(block, 1, metadata);
            if (block.getMaterial() == Material.glass && !is.getUnlocalizedName().contains("pane")) {
               return true;
            } else {
               Iterator var3 = glassList.iterator();

               ItemStack is2;
               do {
                  if (!var3.hasNext()) {
                     return false;
                  }

                  is2 = (ItemStack)var3.next();
               } while(!is2.getUnlocalizedName().equals(is.getUnlocalizedName()));

               return true;
            }
         }
      } else {
         return false;
      }
   }

   public static boolean areTankBlocksValid(ExtendedBlock bottomBlock, ExtendedBlock topBlock, World world, Position3D bottomPos) {
      if (!isValidTankBlock(world, bottomPos, bottomBlock)) {
         return false;
      } else {
         switch(FancyFluidStorage.instance.TANK_FRAME_MODE) {
         case SAME_BLOCK:
            return bottomBlock.equals(topBlock);
         case DIFFERENT_METADATA:
            return bottomBlock.equalsIgnoreMetadata(topBlock);
         case DIFFERENT_BLOCK:
            return true;
         default:
            return false;
         }
      }
   }

   public static boolean isTileEntityAcceptable(Block block, TileEntity tile) {
      Iterator var2 = validTiles.iterator();

      String name;
      do {
         if (!var2.hasNext()) {
            return false;
         }

         name = (String)var2.next();
      } while(!block.getUnlocalizedName().toLowerCase().contains(name.toLowerCase()));

      return true;
   }

   public static boolean isValidTankBlock(World world, Position3D pos, ExtendedBlock extendedBlock) {
      Block block = extendedBlock.getBlock();
      if (block.hasTileEntity(extendedBlock.getMetadata())) {
         TileEntity tile = world.getTileEntity(pos.getX(), pos.getY(), pos.getZ());
         if (tile != null) {
            return tile instanceof TileEntityTankFrame || isTileEntityAcceptable(block, tile);
         }
      }

      if (blacklistedBlocks.contains(block)) {
         return false;
      } else if (block.getMaterial() == Material.sand) {
         return false;
      } else if (!block.isOpaqueCube()) {
         return false;
      } else if (!block.renderAsNormalBlock()) {
         return false;
      } else if (FancyFluidStorage.instance.TANK_FRAME_MODE == FancyFluidStorage.TankFrameMode.DIFFERENT_BLOCK) {
         return true;
      } else {
         return block.func_149730_j();
      }
   }

   public static boolean canBlockLeak(Block block) {
      Material mat = block.getMaterial();
      return mat.equals(Material.grass) || mat.equals(Material.sponge) || mat.equals(Material.cloth) || mat.equals(Material.clay) || mat.equals(Material.gourd) || mat.equals(Material.sand);
   }

   public static boolean isFluidContainer(ItemStack playerItem) {
      if (playerItem == null) {
         return false;
      } else {
         return FluidContainerRegistry.isContainer(playerItem) || playerItem.getItem() instanceof IFluidContainerItem;
      }
   }

   public static boolean fluidContainerHandler(World world, int x, int y, int z, TileEntityValve valve, EntityPlayer player) {
      ItemStack current = player.inventory.getCurrentItem();
      if (current == null) {
         return false;
      } else {
         FluidStack liquid;
         if (FluidContainerRegistry.isContainer(current)) {
            liquid = FluidContainerRegistry.getFluidForFilledItem(current);
            if (liquid != null) {
               int qty = valve.fillFromContainer(ForgeDirection.UNKNOWN, liquid, true);
               if (qty != 0 && !player.capabilities.isCreativeMode) {
                  if (current.stackSize > 1) {
                     if (!player.inventory.addItemStackToInventory(FluidContainerRegistry.drainFluidContainer(current))) {
                        player.dropPlayerItemWithRandomChoice(FluidContainerRegistry.drainFluidContainer(current), false);
                     }

                     player.inventory.setInventorySlotContents(player.inventory.currentItem, consumeItem(current));
                  } else {
                     player.inventory.setInventorySlotContents(player.inventory.currentItem, FluidContainerRegistry.drainFluidContainer(current));
                  }
               }

               return true;
            }

            liquid = valve.getTankInfo(ForgeDirection.UNKNOWN)[0].fluid;
            if (liquid != null) {
               ItemStack filled = FluidContainerRegistry.fillFluidContainer(liquid, current);
               liquid = FluidContainerRegistry.getFluidForFilledItem(filled);
               if (liquid != null) {
                  if (!player.capabilities.isCreativeMode) {
                     if (current.stackSize > 1) {
                        if (!player.inventory.addItemStackToInventory(filled)) {
                           return false;
                        }

                        player.inventory.setInventorySlotContents(player.inventory.currentItem, consumeItem(current));
                     } else {
                        player.inventory.setInventorySlotContents(player.inventory.currentItem, consumeItem(current));
                        player.inventory.setInventorySlotContents(player.inventory.currentItem, filled);
                     }
                  }

                  valve.drain(ForgeDirection.UNKNOWN, liquid.amount, true);
                  return true;
               }
            }
         } else if (current.getItem() instanceof IFluidContainerItem) {
            if (current.stackSize != 1) {
               return false;
            }

            if (!world.isRemote) {
               IFluidContainerItem container = (IFluidContainerItem)current.getItem();
               liquid = container.getFluid(current);
               FluidStack tankLiquid = valve.getTankInfo(ForgeDirection.UNKNOWN)[0].fluid;
               boolean mustDrain = liquid == null || liquid.amount == 0;
               boolean mustFill = tankLiquid == null || tankLiquid.amount == 0;
               if (!mustDrain || !mustFill) {
                  int qty;
                  if (!mustDrain && player.isSneaking()) {
                     if (liquid.amount > 0) {
                        qty = valve.fill(ForgeDirection.UNKNOWN, liquid, false);
                        valve.fill(ForgeDirection.UNKNOWN, container.drain(current, qty, true), true);
                     }
                  } else {
                     liquid = valve.drain(ForgeDirection.UNKNOWN, 1000, false);
                     qty = container.fill(current, liquid, true);
                     valve.drain(ForgeDirection.UNKNOWN, qty, true);
                  }
               }
            }

            return true;
         }

         return false;
      }
   }

   public static ItemStack consumeItem(ItemStack stack) {
      if (stack.stackSize == 1) {
         return stack.getItem().hasContainerItem(stack) ? stack.getItem().getContainerItem(stack) : null;
      } else {
         stack.splitStack(1);
         return stack;
      }
   }

   private static Map getBlocksBetweenPoints(World world, Position3D pos1, Position3D pos2) {
      Map blocks = new HashMap();
      Position3D distance = pos2.getDistance(pos1);
      int dX = distance.getX();
      int dY = distance.getY();
      int dZ = distance.getZ();

      for(int x = 0; x <= dX; ++x) {
         for(int y = 0; y <= dY; ++y) {
            for(int z = 0; z <= dZ; ++z) {
               Position3D pos = new Position3D(pos1.getX() + x, pos1.getY() + y, pos1.getZ() + z);
               blocks.put(pos, new ExtendedBlock(world.getBlock(pos.getX(), pos.getY(), pos.getZ()), world.getBlockMetadata(pos.getX(), pos.getY(), pos.getZ())));
            }
         }
      }

      return blocks;
   }

   public static Map[] getTankFrame(World world, Position3D bottomDiag, Position3D topDiag) {
      Map[] maps = new HashMap[]{new HashMap(), new HashMap(), new HashMap()};
      int x1 = bottomDiag.getX();
      int y1 = bottomDiag.getY();
      int z1 = bottomDiag.getZ();
      int x2 = topDiag.getX();
      int y2 = topDiag.getY();
      int z2 = topDiag.getZ();
      Iterator var10 = getBlocksBetweenPoints(world, new Position3D(x1, y1, z1), new Position3D(x2, y2, z2)).entrySet().iterator();

      while(true) {
         while(true) {
            while(var10.hasNext()) {
               Entry e = (Entry)var10.next();
               Position3D p = (Position3D)e.getKey();
               if ((p.getX() != x1 && p.getX() != x2 || p.getY() != y1 && p.getY() != y2) && (p.getX() != x1 && p.getX() != x2 || p.getZ() != z1 && p.getZ() != z2) && (p.getY() != y1 && p.getY() != y2 || p.getZ() != z1 && p.getZ() != z2)) {
                  if (p.getX() != x1 && p.getX() != x2 && p.getY() != y1 && p.getY() != y2 && p.getZ() != z1 && p.getZ() != z2) {
                     maps[2].put(p, e.getValue());
                  } else {
                     maps[1].put(p, e.getValue());
                  }
               } else {
                  maps[0].put(p, e.getValue());
               }
            }

            return maps;
         }
      }
   }

   public static String intToFancyNumber(int number) {
      return NumberFormat.getIntegerInstance().format((long)number);
   }
}
