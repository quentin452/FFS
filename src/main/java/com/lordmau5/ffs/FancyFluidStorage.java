package com.lordmau5.ffs;

import com.lordmau5.ffs.blocks.BlockTankFrame;
import com.lordmau5.ffs.blocks.BlockValve;
import com.lordmau5.ffs.network.NetworkHandler;
import com.lordmau5.ffs.proxy.CommonProxy;
import com.lordmau5.ffs.proxy.GuiHandler;
import com.lordmau5.ffs.tile.TileEntityTankFrame;
import com.lordmau5.ffs.tile.TileEntityValve;
import com.lordmau5.ffs.util.GenericUtil;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

@Mod(
   modid = "FFS",
   name = "Fancy Fluid Storage",
   dependencies = "after:waila;after:chisel;after:OpenComputers;after:ComputerCraft;after:BuildCraftAPI|Transport;after:funkylocomotion"
)
public class FancyFluidStorage {
   public static final String modId = "FFS";
   public static BlockValve blockValve;
   public static BlockTankFrame blockTankFrame;
   public static Configuration config;
   @Instance("FFS")
   public static FancyFluidStorage instance;
   @SidedProxy(
      clientSide = "com.lordmau5.ffs.proxy.ClientProxy",
      serverSide = "com.lordmau5.ffs.proxy.CommonProxy"
   )
   public static CommonProxy proxy;
   public boolean ANONYMOUS_STATISTICS = true;
   public int MB_PER_TANK_BLOCK = 16000;
   public boolean INSIDE_CAPACITY = false;
   public int MAX_SIZE = 13;
   public int MIN_BURNABLE_TEMPERATURE = 1300;
   public boolean SET_WORLD_ON_FIRE = true;
   public boolean SHOULD_TANKS_LEAK = true;
   public boolean TANK_RENDER_INSIDE = true;
   public FancyFluidStorage.TankFrameMode TANK_FRAME_MODE;

   public FancyFluidStorage() {
      this.TANK_FRAME_MODE = FancyFluidStorage.TankFrameMode.SAME_BLOCK;
   }

   public void loadConfig() {
      config.load();
      Property usageStatistics = config.get("general", "usageStatistics", true);
      usageStatistics.comment = "Should the mod send anonymous usage statistics to GameAnalytics?\nThis allows us to evaluate interesting statistics :)\nDefault: true";
      this.ANONYMOUS_STATISTICS = usageStatistics.getBoolean(true);
      Property mbPerTankProp = config.get("general", "mbPerVirtualTank", 16000);
      mbPerTankProp.comment = "How many millibuckets can each block within the tank store? (Has to be higher than 1!)\nDefault: 16000";
      this.MB_PER_TANK_BLOCK = Math.max(1, Math.min(Integer.MAX_VALUE, mbPerTankProp.getInt(16000)));
      if (mbPerTankProp.getInt(16000) < 1 || mbPerTankProp.getInt(16000) > Integer.MAX_VALUE) {
         mbPerTankProp.set(16000);
      }

      Property insideCapacityProp = config.get("general", "onlyCountInsideCapacity", true);
      insideCapacityProp.comment = "Should tank capacity only count the interior air blocks, rather than including the frame?\nDefault: true";
      this.INSIDE_CAPACITY = insideCapacityProp.getBoolean(true);
      Property maxSizeProp = config.get("general", "maxSize", 13);
      maxSizeProp.comment = "Define the maximum size a tank can have. This includes the whole tank, including the frame!\nMinimum: 3, Maximum: 32\nDefault: 13";
      this.MAX_SIZE = Math.max(3, Math.min(maxSizeProp.getInt(13), 32));
      if (maxSizeProp.getInt(13) < 3 || maxSizeProp.getInt(13) > 32) {
         maxSizeProp.set(13);
      }

      Property tankFrameModeProp = config.get("general", "tankFrameMode", 1);
      tankFrameModeProp.comment = "Declare which mode you want the tank frames to be.\n0 = Only the same block with the same metadata is allowed\n1 = Only the same block is allowed, but the metadata can be different\n2 = Allow any block\nDefault: 1";
      int mode = tankFrameModeProp.getInt(1);
      if (mode < 0 || mode > 2) {
         mode = 1;
         tankFrameModeProp.set(1);
      }

      this.TANK_FRAME_MODE = FancyFluidStorage.TankFrameMode.values()[mode];
      Property minBurnProp = config.get("general", "minimumBurnableTemperature", 1300);
      minBurnProp.comment = "At which temperature should a tank start burning on a random occasion? (Has to be positive!)\nThis only applies to blocks that are flammable, like Wood or Wool.\nDefault: 1300 (Temperature of Lava)\n0 to disable.";
      this.MIN_BURNABLE_TEMPERATURE = Math.max(0, minBurnProp.getInt(1300));
      if (minBurnProp.getInt(1300) < 0) {
         minBurnProp.set(1300);
      }

      Property setWorldOnFireProp = config.get("general", "setWorldOnFire", true);
      setWorldOnFireProp.comment = "Do you want to set the world on fire? Or do you just want to create a flame in my heart?\n(Don't worry, this is harmless :))\nDefault: true";
      this.SET_WORLD_ON_FIRE = setWorldOnFireProp.getBoolean(true);
      Property tanksLeakProp = config.get("general", "shouldTanksLeak", true);
      tanksLeakProp.comment = "Should tanks with leaky materials start leaking randomly?\nDefault: true";
      this.SHOULD_TANKS_LEAK = tanksLeakProp.getBoolean(true);
      Property tankRenderInside = config.get("general", "tanksRenderInsideOnly", true);
      tankRenderInside.comment = "Should tanks only render the inside as fluid or extend to the frame-sides?\nDefault: true";
      this.TANK_RENDER_INSIDE = tankRenderInside.getBoolean(true);
      if (config.hasChanged()) {
         config.save();
      }

   }

   @EventHandler
   public void preInit(FMLPreInitializationEvent event) {
      proxy.preInit();
      config = new Configuration(event.getSuggestedConfigurationFile());
      this.loadConfig();
      GameRegistry.registerBlock(blockValve = new BlockValve(), "blockValve");
      GameRegistry.registerBlock(blockTankFrame = new BlockTankFrame(), "blockTankFrame");
      GameRegistry.registerTileEntity(TileEntityValve.class, "tileEntityValve");
      GameRegistry.registerTileEntity(TileEntityTankFrame.class, "tileEntityTankFrame");
      NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());
      NetworkHandler.registerChannels(event.getSide());
   }

   @EventHandler
   public void init(FMLInitializationEvent event) {
      GameRegistry.addRecipe(new ItemStack(blockValve), new Object[]{"IGI", "GBG", "IGI", 'I', Items.iron_ingot, 'G', Blocks.iron_bars, 'B', Items.bucket});
      proxy.init();
   }

   @EventHandler
   public void postInit(FMLPostInitializationEvent event) {
      GenericUtil.init();
   }

   public static enum TankFrameMode {
      SAME_BLOCK,
      DIFFERENT_METADATA,
      DIFFERENT_BLOCK;
   }
}
