package com.lordmau5.ffs.proxy;

import com.lordmau5.ffs.client.gui.GuiValve;
import com.lordmau5.ffs.tile.TileEntityTankFrame;
import com.lordmau5.ffs.tile.TileEntityValve;
import cpw.mods.fml.common.network.IGuiHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class GuiHandler implements IGuiHandler {
   public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
      return null;
   }

   public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
      TileEntity tile = world.getTileEntity(x, y, z);
      if (tile == null) {
         return null;
      } else if (tile instanceof TileEntityValve) {
         return new GuiValve((TileEntityValve)tile, false);
      } else {
         return tile instanceof TileEntityTankFrame ? new GuiValve(((TileEntityTankFrame)tile).getValve(), true) : null;
      }
   }
}
