package com.lordmau5.ffs.proxy;

import com.lordmau5.ffs.compat.CCPeripheralProvider;
import com.lordmau5.ffs.compat.RiMEventHandler;
import cpw.mods.fml.common.Loader;
import me.planetguy.remaininmotion.api.event.EventManager;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;

public class CommonProxy {
   public boolean BUILDCRAFT_LOADED;
   public IIcon tex_Valve;
   public IIcon tex_ValveItem;
   public IIcon[] tex_SlaveValve;
   public IIcon[] tex_MasterValve;

   public void preInit() {
   }

   public void registerIcons(IIconRegister iR) {
   }

   public void init() {
      this.BUILDCRAFT_LOADED = Loader.isModLoaded("BuildCraftAPI|Transport");
      if (Loader.isModLoaded("ComputerCraft")) {
         (new CCPeripheralProvider()).register();
      }

      if (Loader.isModLoaded("JAKJ_RedstoneInMotion")) {
         EventManager.registerEventHandler(new RiMEventHandler());
      }

   }
}
