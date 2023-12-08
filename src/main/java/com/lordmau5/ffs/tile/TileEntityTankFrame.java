package com.lordmau5.ffs.tile;

import com.lordmau5.ffs.util.ExtendedBlock;
import com.lordmau5.ffs.util.Position3D;
import cpw.mods.fml.common.Optional.Interface;
import cpw.mods.fml.common.Optional.InterfaceList;
import cpw.mods.fml.common.Optional.Method;
import framesapi.IMoveCheck;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

@InterfaceList({@Interface(
   iface = "framesapi.IMoveCheck",
   modid = "funkylocomotion"
)})
public class TileEntityTankFrame extends TileEntity implements IMoveCheck {
   private ExtendedBlock block;
   private Position3D valvePos;
   private TileEntityValve masterValve;
   private boolean wantsUpdate = false;

   public void initialize(TileEntityValve masterValve, ExtendedBlock block) {
      this.masterValve = masterValve;
      this.block = block;
   }

   public boolean isFrameInvalid() {
      TileEntity tile = this.worldObj.getTileEntity(this.xCoord, this.yCoord, this.zCoord);
      return tile == null || !(tile instanceof TileEntityTankFrame) || tile != this;
   }

   public List getNeighborBlockOrAir(Block block) {
      List dirs = new ArrayList();
      ForgeDirection[] var3 = ForgeDirection.VALID_DIRECTIONS;
      int var4 = var3.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         ForgeDirection dr = var3[var5];
         if (block == Blocks.air) {
            if (this.worldObj.isAirBlock(this.xCoord + dr.offsetX, this.yCoord + dr.offsetY, this.zCoord + dr.offsetZ)) {
               dirs.add(dr);
            }
         } else {
            Block otherBlock = this.worldObj.getBlock(this.xCoord + dr.offsetX, this.yCoord + dr.offsetY, this.zCoord + dr.offsetZ);
            if (block == otherBlock || this.worldObj.isAirBlock(this.xCoord + dr.offsetX, this.yCoord + dr.offsetY, this.zCoord + dr.offsetZ)) {
               dirs.add(dr);
            }
         }
      }

      return dirs;
   }

   public boolean tryBurning() {
      Block block = this.getBlock().getBlock();
      if (block == null) {
         return false;
      } else {
         List air = this.getNeighborBlockOrAir(Blocks.air);
         Iterator var3 = air.iterator();

         ForgeDirection dr;
         do {
            if (!var3.hasNext()) {
               return false;
            }

            dr = (ForgeDirection)var3.next();
         } while(!block.isFlammable(this.worldObj, this.xCoord, this.yCoord, this.zCoord, dr));

         this.worldObj.setBlock(this.xCoord + dr.offsetX, this.yCoord + dr.offsetY, this.zCoord + dr.offsetZ, Blocks.fire, 0, 3);
         return true;
      }
   }

   public void breakFrame() {
      if (!this.isFrameInvalid()) {
         this.worldObj.removeTileEntity(this.xCoord, this.yCoord, this.zCoord);
         if (this.block != null && this.block.getBlock() != null) {
            this.worldObj.setBlock(this.xCoord, this.yCoord, this.zCoord, this.block.getBlock(), this.block.getMetadata(), 3);
         } else {
            this.worldObj.setBlockToAir(this.xCoord, this.yCoord, this.zCoord);
         }

      }
   }

   public void onBreak() {
      if (this.worldObj != null && !this.worldObj.isRemote && this.getValve() != null) {
         this.masterValve.breakTank(this);
      }

   }

   public void setValvePos(Position3D valvePos) {
      this.valvePos = valvePos;
      this.masterValve = null;
   }

   public TileEntityValve getValve() {
      if (this.masterValve == null && this.valvePos != null) {
         TileEntity tile = this.worldObj.getTileEntity(this.valvePos.getX(), this.valvePos.getY(), this.valvePos.getZ());
         this.masterValve = tile instanceof TileEntityValve ? (TileEntityValve)tile : null;
      }

      return this.masterValve;
   }

   public void setBlock(ExtendedBlock block) {
      this.block = block;
   }

   public ExtendedBlock getBlock() {
      return this.block;
   }

   public void readFromNBT(NBTTagCompound tag) {
      super.readFromNBT(tag);
      if (tag.hasKey("valveX")) {
         this.setValvePos(new Position3D(tag.getInteger("valveX"), tag.getInteger("valveY"), tag.getInteger("valveZ")));
      }

      if (tag.hasKey("blockId")) {
         this.block = new ExtendedBlock(Block.getBlockById(tag.getInteger("blockId")), tag.getInteger("metadata"));
      }

   }

   public void writeToNBT(NBTTagCompound tag) {
      super.writeToNBT(tag);
      if (this.getValve() != null) {
         tag.setInteger("valveX", this.getValve().xCoord);
         tag.setInteger("valveY", this.getValve().yCoord);
         tag.setInteger("valveZ", this.getValve().zCoord);
      }

      if (this.getBlock() != null) {
         tag.setInteger("blockId", Block.getIdFromBlock(this.getBlock().getBlock()));
         tag.setInteger("metadata", this.getBlock().getMetadata());
      }

   }

   public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
      this.readFromNBT(pkt.func_148857_g());
   }

   public Packet getDescriptionPacket() {
      NBTTagCompound tag = new NBTTagCompound();
      this.writeToNBT(tag);
      return new S35PacketUpdateTileEntity(this.xCoord, this.yCoord, this.zCoord, 0, tag);
   }

   public void markForUpdate() {
      if (this.worldObj == null) {
         this.wantsUpdate = true;
      } else {
         this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
      }
   }

   public void updateEntity() {
      if (this.wantsUpdate) {
         this.markForUpdate();
         this.wantsUpdate = false;
      }

   }

   @Method(
      modid = "funkylocomotion"
   )
   public boolean canMove(World worldObj, int x, int y, int z) {
      return false;
   }
}
