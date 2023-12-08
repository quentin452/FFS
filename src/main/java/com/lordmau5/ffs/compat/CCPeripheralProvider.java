package com.lordmau5.ffs.compat;

import com.lordmau5.ffs.tile.TileEntityValve;
import cpw.mods.fml.common.Optional.Interface;
import cpw.mods.fml.common.Optional.Method;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralProvider;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

@Interface(
   iface = "dan200.computercraft.api.peripheral.IPeripheralProvider",
   modid = "ComputerCraft"
)
public class CCPeripheralProvider implements IPeripheralProvider {
   public void register() {
      ComputerCraftAPI.registerPeripheralProvider(this);
   }

   @Method(
      modid = "ComputerCraft"
   )
   public IPeripheral getPeripheral(World world, int x, int y, int z, int side) {
      TileEntity tile = world.getTileEntity(x, y, z);
      return tile != null && tile instanceof TileEntityValve ? (IPeripheral)tile : null;
   }
}
