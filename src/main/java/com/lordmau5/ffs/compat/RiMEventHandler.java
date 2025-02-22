package com.lordmau5.ffs.compat;

import com.lordmau5.ffs.tile.TileEntityTankFrame;
import com.lordmau5.ffs.tile.TileEntityValve;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import me.planetguy.remaininmotion.api.event.BlockPreMoveEvent;
import me.planetguy.remaininmotion.api.event.BlockRotateEvent;
import me.planetguy.remaininmotion.api.event.MotionFinalizeEvent;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

public class RiMEventHandler {
   @SubscribeEvent
   public void motionFinalize(MotionFinalizeEvent event) {
      TileEntity tile = event.location.entity();
      if (tile instanceof TileEntityValve) {
         TileEntityValve valve = (TileEntityValve)tile;
         if (valve.isMaster()) {
            valve.initiated = true;
            valve.initialWaitTick = 0;
            valve.calculateInside();
            valve.updateCornerFrames();
         }
      } else if (tile instanceof TileEntityTankFrame) {
         TileEntityTankFrame frame = (TileEntityTankFrame)tile;
         frame.breakFrame();
      }

   }

   @SubscribeEvent
   public void blockPreMove(BlockPreMoveEvent event) {
      TileEntity tile = event.location.entity();
      if (tile instanceof TileEntityTankFrame) {
         TileEntityTankFrame frame = (TileEntityTankFrame)tile;
         frame.onBreak();
         frame.breakFrame();
      }

   }

   @SubscribeEvent
   public void tileRotation(BlockRotateEvent event) {
      TileEntity tile = event.location.entity();
      if (tile instanceof TileEntityValve) {
         TileEntityValve valve = (TileEntityValve)tile;
         if (valve.isMaster()) {
            ForgeDirection rot = valve.getInside().getRotation(event.axis.getRotation(event.axis));
            valve.setInside(rot);
         }
      } else if (tile instanceof TileEntityTankFrame) {
         TileEntityTankFrame frame = (TileEntityTankFrame)tile;
         frame.breakFrame();
      }

   }
}
