package com.lordmau5.ffs.tile;

import buildcraft.api.transport.IPipeConnection;
import buildcraft.api.transport.IPipeTile;
import buildcraft.api.transport.IPipeTile.PipeType;
import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.blocks.BlockTankFrame;
import com.lordmau5.ffs.util.ExtendedBlock;
import com.lordmau5.ffs.util.GenericUtil;
import com.lordmau5.ffs.util.Position3D;
import cpw.mods.fml.common.Optional.Interface;
import cpw.mods.fml.common.Optional.InterfaceList;
import cpw.mods.fml.common.Optional.Method;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import framesapi.IMoveCheck;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedPeripheral;
import li.cil.oc.api.network.SimpleComponent;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;
import net.minecraftforge.fluids.IFluidTank;

@InterfaceList({@Interface(
   iface = "buildcraft.api.transport.IPipeConnection",
   modid = "BuildCraftAPI|Transport"
), @Interface(
   iface = "dan200.computercraft.api.peripheral.IPeripheral",
   modid = "ComputerCraft"
), @Interface(
   iface = "li.cil.oc.api.network.SimpleComponent",
   modid = "OpenComputers"
), @Interface(
   iface = "li.cil.oc.api.network.ManagedPeripheral",
   modid = "OpenComputers"
), @Interface(
   iface = "framesapi.IMoveCheck",
   modid = "funkylocomotion"
)})
public class TileEntityValve extends TileEntity implements IFluidTank, IFluidHandler, IPipeConnection, IPeripheral, SimpleComponent, ManagedPeripheral, IMoveCheck {
   private final int maxSize;
   protected int mbPerVirtualTank;
   protected int minBurnableTemp;
   private int frameBurnability;
   private String valveName;
   public boolean isValid;
   private boolean isMaster;
   private Position3D masterValvePos;
   public boolean initiated;
   private boolean needsUpdate;
   public int tankHeight;
   public int valveHeightPosition;
   private boolean autoOutput;
   private int fluidIntake;
   private int fluidOuttake;
   private int rainIntake;
   private int randomBurnTicks;
   private int randomLeakTicks;
   private ForgeDirection inside;
   private TileEntityValve master;
   public List tankFrames;
   public List otherValves;
   private Map[] maps;
   private int[] length;
   public Position3D bottomDiagFrame;
   public Position3D topDiagFrame;
   public int initialWaitTick;
   private FluidStack fluidStack;
   private int fluidTemperature;
   private int fluidCapacity;
   private int lastComparatorOut;

   public TileEntityValve() {
      this.maxSize = FancyFluidStorage.instance.MAX_SIZE;
      this.mbPerVirtualTank = FancyFluidStorage.instance.MB_PER_TANK_BLOCK;
      this.minBurnableTemp = FancyFluidStorage.instance.MIN_BURNABLE_TEMPERATURE;
      this.frameBurnability = 0;
      this.valveName = "";
      this.tankHeight = 0;
      this.valveHeightPosition = 0;
      this.randomBurnTicks = 100;
      this.randomLeakTicks = 1200;
      this.inside = ForgeDirection.UNKNOWN;
      this.length = new int[6];
      this.initialWaitTick = 20;
      this.fluidTemperature = 0;
      this.fluidCapacity = 0;
      this.lastComparatorOut = 0;
      this.tankFrames = new ArrayList();
      this.otherValves = new ArrayList();
   }

   public void validate() {
      super.validate();
      this.initiated = true;
      this.initialWaitTick = 20;
   }

   public void updateEntity() {
      if (!this.worldObj.isRemote) {
         if (this.needsUpdate) {
            this.getMaster().markForUpdate(true);
            this.needsUpdate = false;
            if (this.fluidIntake != 0) {
               this.fluidIntake = 0;
            }

            if (this.fluidOuttake != 0) {
               this.fluidOuttake = 0;
            }

            if (this.rainIntake != 0) {
               this.rainIntake = 0;
            }
         }

         int id;
         if (this.initiated && this.isMaster()) {
            if (this.bottomDiagFrame != null && this.topDiagFrame != null) {
               Chunk chunkBottom = this.worldObj.getChunkFromBlockCoords(this.bottomDiagFrame.getX(), this.bottomDiagFrame.getZ());
               Chunk chunkTop = this.worldObj.getChunkFromBlockCoords(this.topDiagFrame.getX(), this.topDiagFrame.getZ());
               Position3D pos_chunkBottom = new Position3D(chunkBottom.xPosition, 0, chunkBottom.zPosition);
               Position3D pos_chunkTop = new Position3D(chunkTop.xPosition, 0, chunkTop.zPosition);
               Position3D diff = pos_chunkTop.getDistance(pos_chunkBottom);
               id = 0;

               while(true) {
                  if (id > diff.getX()) {
                     this.updateBlockAndNeighbors();
                     break;
                  }

                  for(int z = 0; z <= diff.getZ(); ++z) {
                     this.worldObj.getChunkProvider().loadChunk(pos_chunkTop.getX() + id, pos_chunkTop.getZ() + z);
                  }

                  ++id;
               }
            }

            if (this.initialWaitTick-- <= 0) {
               this.initiated = false;
               this.buildTank(this.inside);
               return;
            }
         }

         if (this.isValid()) {
            if (!this.isMaster() && this.getMaster() == null) {
               this.setValid(false);
               this.updateBlockAndNeighbors();
            }

            if (this.getFluid() != null) {
               if ((this.getAutoOutput() || this.valveHeightPosition == 0) && this.getFluidAmount() != 0) {
                  float height = (float)this.getFluidAmount() / (float)this.getCapacity();
                  boolean isNegativeDensity = this.getFluid().getFluid().getDensity(this.getFluid()) < 0;
                  if (GenericUtil.canAutoOutput(height, this.getTankHeight(), this.valveHeightPosition, isNegativeDensity)) {
                     ForgeDirection out = this.inside.getOpposite();
                     TileEntity tile = this.worldObj.getTileEntity(this.xCoord + out.offsetX, this.yCoord + out.offsetY, this.zCoord + out.offsetZ);
                     if (tile != null && (tile instanceof TileEntityValve || this.getAutoOutput() || this.valveHeightPosition != 0)) {
                        int maxAmount = 0;
                        if (tile instanceof TileEntityValve) {
                           maxAmount = 1000;
                        } else if (tile instanceof IFluidHandler) {
                           maxAmount = 50;
                        }

                        if (maxAmount != 0) {
                           IFluidHandler handler = (IFluidHandler)tile;
                           FluidStack fillStack = this.getFluid().copy();
                           fillStack.amount = Math.min(this.getFluidAmount(), maxAmount);
                           if (handler.fill(this.inside, fillStack, false) > 0) {
                              this.drain(handler.fill(this.inside, fillStack, true), true);
                           }
                        }
                     }
                  }
               }

               if (this.getFluid() != null && this.getFluid().getFluid() == FluidRegistry.WATER && this.worldObj.isRaining()) {
                  int rate = (int)Math.floor((double)(this.worldObj.rainingStrength * 5.0F * this.worldObj.getBiomeGenForCoords(this.xCoord, this.zCoord).rainfall));
                  if (this.yCoord == this.worldObj.getPrecipitationHeight(this.xCoord, this.zCoord) - 1) {
                     FluidStack waterStack = this.getFluid().copy();
                     waterStack.amount = rate * 10;
                     this.rainIntake += waterStack.amount;
                     this.fill(waterStack, true);
                  }
               }

               if (this.isMaster()) {
                  int rand;
                  Random random;
                  int amt;
                  if (this.minBurnableTemp > 0 && this.fluidTemperature >= this.minBurnableTemp && this.frameBurnability > 0 && this.randomBurnTicks-- <= 0) {
                     this.randomBurnTicks = 100;
                     random = new Random();
                     amt = this.fluidTemperature - this.minBurnableTemp;
                     int chanceOfBurnability = 300 - this.frameBurnability;
                     rand = random.nextInt(300) + amt + (int)Math.floor((double)((float)this.getFluidAmount() / (float)this.getCapacity() * 300.0F));
                     if (rand >= chanceOfBurnability) {
                        boolean successfullyBurned = false;
                        List remainingFrames = new ArrayList();
                        remainingFrames.addAll(this.tankFrames);
                        ArrayList removingFrames = new ArrayList();

                        TileEntityTankFrame frame;
                        while(!successfullyBurned && remainingFrames.size() != 0) {
                           boolean couldBurnOne = false;

                           for(rand = 0; rand < Math.min(10, remainingFrames.size()); ++rand) {
                              id = random.nextInt(remainingFrames.size());
                              frame = (TileEntityTankFrame)remainingFrames.get(id);
                              couldBurnOne = frame.tryBurning();
                              if (!couldBurnOne) {
                                 removingFrames.add(frame);
                              }
                           }

                           remainingFrames.removeAll(removingFrames);
                           removingFrames.clear();
                           if (couldBurnOne) {
                              successfullyBurned = true;
                           }
                        }

                        if (!successfullyBurned) {
                           remainingFrames.clear();
                           remainingFrames.addAll(this.tankFrames);
                           List firePos = new ArrayList();
                           rand = 0;

                           while(rand < 3 && remainingFrames.size() != 0) {
                              id = random.nextInt(remainingFrames.size());
                              frame = (TileEntityTankFrame)remainingFrames.get(id);
                              if (frame.getBlock().getBlock().isFlammable(this.worldObj, frame.xCoord, frame.yCoord, frame.zCoord, ForgeDirection.UNKNOWN)) {
                                 firePos.add(new Position3D(frame.xCoord, frame.yCoord, frame.zCoord));
                                 ++rand;
                              } else {
                                 remainingFrames.remove(id);
                              }
                           }

                           Iterator var40 = firePos.iterator();

                           while(var40.hasNext()) {
                              Position3D pos = (Position3D)var40.next();
                              if (this.worldObj.getBlock(pos.getX(), pos.getY(), pos.getZ()).isFlammable(this.worldObj, pos.getX(), pos.getY(), pos.getZ(), ForgeDirection.UNKNOWN)) {
                                 this.worldObj.setBlock(pos.getX(), pos.getY(), pos.getZ(), Blocks.fire);
                              }
                           }
                        }

                        this.frameBurnability = 0;
                        if (FancyFluidStorage.instance.SET_WORLD_ON_FIRE) {
                           this.worldObj.playSoundEffect((double)this.xCoord + 0.5D, (double)this.yCoord + 0.5D, (double)this.zCoord + 0.5D, "FFS:fire", 1.0F, this.worldObj.rand.nextFloat() * 0.1F + 0.9F);
                        }
                     }
                  }

                  if (FancyFluidStorage.instance.SHOULD_TANKS_LEAK && this.randomLeakTicks-- <= 0 && this.fluidStack != null && this.fluidStack.getFluid().canBePlacedInWorld()) {
                     this.randomLeakTicks = 1200;
                     random = new Random();
                     amt = random.nextInt(3) + 1;
                     List validFrames = new ArrayList();
                     List remainingFrames = new ArrayList();
                     remainingFrames.addAll(this.tankFrames);
                     int i = 0;

                     while(true) {
                        while(i < amt && remainingFrames.size() != 0) {
                           id = random.nextInt(remainingFrames.size());
                           TileEntityTankFrame frame = (TileEntityTankFrame)remainingFrames.get(id);
                           Block block = frame.getBlock().getBlock();
                           if (GenericUtil.canBlockLeak(block) && !frame.getNeighborBlockOrAir(this.fluidStack.getFluid().getBlock()).isEmpty() && block.getBlockHardness(this.worldObj, frame.xCoord, frame.yCoord, frame.zCoord) <= 1.0F) {
                              validFrames.add(frame);
                              ++i;
                           } else {
                              remainingFrames.remove(id);
                           }
                        }

                        Iterator var34 = validFrames.iterator();

                        while(var34.hasNext()) {
                           TileEntityTankFrame frame = (TileEntityTankFrame)var34.next();
                           Block block = frame.getBlock().getBlock();
                           int hardness = (int)Math.ceil((double)(block.getBlockHardness(this.worldObj, frame.xCoord, frame.yCoord, frame.zCoord) * 100.0F));
                           rand = random.nextInt(hardness) + 1;
                           id = (int)Math.ceil((double)(50.0F * ((float)this.getFluidAmount() / (float)this.getCapacity())));
                           if (rand >= hardness - id) {
                              List dirs = frame.getNeighborBlockOrAir(this.fluidStack.getFluid().getBlock());
                              if (dirs.size() != 0) {
                                 ForgeDirection leakDir;
                                 if (dirs.size() > 1) {
                                    leakDir = (ForgeDirection)dirs.get(random.nextInt(dirs.size()));
                                 } else {
                                    leakDir = (ForgeDirection)dirs.get(0);
                                 }

                                 Position3D leakPos = new Position3D(frame.xCoord + leakDir.offsetX, frame.yCoord + leakDir.offsetY, frame.zCoord + leakDir.offsetZ);
                                 if (!this.maps[2].containsKey(leakPos) && this.fluidStack.amount >= 1000) {
                                    this.worldObj.setBlock(frame.xCoord + leakDir.offsetX, frame.yCoord + leakDir.offsetY, frame.zCoord + leakDir.offsetZ, this.fluidStack.getFluid().getBlock(), 0, 3);
                                    this.worldObj.notifyBlockOfNeighborChange(frame.xCoord + leakDir.offsetX, frame.yCoord + leakDir.offsetY, frame.zCoord + leakDir.offsetZ, this.fluidStack.getFluid().getBlock());
                                    this.drain(1000, true);
                                 }
                              }
                           }
                        }
                        break;
                     }
                  }
               }

            }
         }
      }
   }

   private List getAllValves() {
      if (!this.isMaster() && this.getMaster() != this) {
         return this.getMaster().getAllValves();
      } else {
         List valves = new ArrayList();
         valves.add(this);
         if (this.otherValves.isEmpty()) {
            return valves;
         } else {
            Iterator var2 = this.otherValves.iterator();

            while(var2.hasNext()) {
               TileEntityValve valve = (TileEntityValve)var2.next();
               valves.add(valve);
            }

            return valves;
         }
      }
   }

   private List getValvesByName(String name) {
      if (!this.isMaster()) {
         return this.getMaster().getValvesByName(name);
      } else {
         List valves = new ArrayList();
         if (this.getAllValves().isEmpty()) {
            return valves;
         } else {
            Iterator var3 = this.getAllValves().iterator();

            while(var3.hasNext()) {
               TileEntityValve valve = (TileEntityValve)var3.next();
               if (valve.getValveName().toLowerCase().equals(name.toLowerCase())) {
                  valves.add(valve);
               }
            }

            return valves;
         }
      }
   }

   public String getValveName() {
      if (this.valveName.isEmpty()) {
         this.setValveName(GenericUtil.getUniqueValveName(this));
      }

      return this.valveName;
   }

   public void setValveName(String valveName) {
      this.valveName = valveName;
   }

   public void setNeedsUpdate() {
      this.needsUpdate = true;
   }

   public int getTankHeight() {
      return this.isMaster() ? this.tankHeight : this.getMaster().tankHeight;
   }

   public void setInside(ForgeDirection inside) {
      this.inside = inside;
   }

   public ForgeDirection getInside() {
      return this.inside;
   }

   public void buildTank(ForgeDirection inside) {
      if (!this.worldObj.isRemote) {
         this.setValid(false);
         this.fluidCapacity = 0;
         this.tankFrames.clear();
         this.otherValves.clear();
         if (inside != null) {
            this.setInside(inside);
         }

         if (this.calculateInside()) {
            if (this.setupTank()) {
               this.initiated = false;
               this.updateBlockAndNeighbors();
            }
         }
      }
   }

   public boolean calculateInside() {
      int xIn = this.xCoord + this.inside.offsetX;
      int yIn = this.yCoord + this.inside.offsetY;
      int zIn = this.zCoord + this.inside.offsetZ;
      ForgeDirection[] var4 = ForgeDirection.VALID_DIRECTIONS;
      int var5 = var4.length;

      for(int var6 = 0; var6 < var5; ++var6) {
         ForgeDirection dr = var4[var6];

         for(int i = 0; i < this.maxSize; ++i) {
            if (!this.worldObj.isAirBlock(xIn + dr.offsetX * i, yIn + dr.offsetY * i, zIn + dr.offsetZ * i)) {
               this.length[dr.ordinal()] = i - 1;
               break;
            }
         }
      }

      for(int i = 0; i < 6; i += 2) {
         if (this.length[i] + this.length[i + 1] > this.maxSize) {
            return false;
         }
      }

      return this.length[0] != -1;
   }

   private void setSlaveValveInside(Map airBlocks, TileEntityValve slave) {
      List possibleAirBlocks = new ArrayList();
      ForgeDirection[] var4 = ForgeDirection.VALID_DIRECTIONS;
      int var5 = var4.length;

      for(int var6 = 0; var6 < var5; ++var6) {
         ForgeDirection dr = var4[var6];
         if (this.worldObj.isAirBlock(slave.xCoord + dr.offsetX, slave.yCoord + dr.offsetY, slave.zCoord + dr.offsetZ)) {
            possibleAirBlocks.add(new Position3D(slave.xCoord + dr.offsetX, slave.yCoord + dr.offsetY, slave.zCoord + dr.offsetZ));
         }
      }

      Position3D insideAir = null;
      Iterator var11 = possibleAirBlocks.iterator();

      while(var11.hasNext()) {
         Position3D pos = (Position3D)var11.next();
         if (airBlocks.containsKey(pos)) {
            insideAir = pos;
            break;
         }
      }

      if (insideAir != null) {
         Position3D dist = insideAir.getDistance(new Position3D(slave.xCoord, slave.yCoord, slave.zCoord));
         ForgeDirection[] var14 = ForgeDirection.VALID_DIRECTIONS;
         int var15 = var14.length;

         for(int var8 = 0; var8 < var15; ++var8) {
            ForgeDirection dr = var14[var8];
            if (dist.equals(new Position3D(dr.offsetX, dr.offsetY, dr.offsetZ))) {
               slave.setInside(dr);
               break;
            }
         }

      }
   }

   public void updateCornerFrames() {
      this.bottomDiagFrame = new Position3D(this.xCoord + this.inside.offsetX + this.length[ForgeDirection.WEST.ordinal()] * ForgeDirection.WEST.offsetX + ForgeDirection.WEST.offsetX, this.yCoord + this.inside.offsetY + this.length[ForgeDirection.DOWN.ordinal()] * ForgeDirection.DOWN.offsetY + ForgeDirection.DOWN.offsetY, this.zCoord + this.inside.offsetZ + this.length[ForgeDirection.NORTH.ordinal()] * ForgeDirection.NORTH.offsetZ + ForgeDirection.NORTH.offsetZ);
      this.topDiagFrame = new Position3D(this.xCoord + this.inside.offsetX + this.length[ForgeDirection.EAST.ordinal()] * ForgeDirection.EAST.offsetX + ForgeDirection.EAST.offsetX, this.yCoord + this.inside.offsetY + this.length[ForgeDirection.UP.ordinal()] * ForgeDirection.UP.offsetY + ForgeDirection.UP.offsetY, this.zCoord + this.inside.offsetZ + this.length[ForgeDirection.SOUTH.ordinal()] * ForgeDirection.SOUTH.offsetZ + ForgeDirection.SOUTH.offsetZ);
   }

   private void fetchMaps() {
      this.maps = GenericUtil.getTankFrame(this.worldObj, this.bottomDiagFrame, this.topDiagFrame);
   }

   private boolean setupTank() {
      this.updateCornerFrames();
      this.fetchMaps();
      this.otherValves = new ArrayList();
      this.tankFrames = new ArrayList();
      Position3D pos = new Position3D(this.xCoord, this.yCoord, this.zCoord);
      this.valveHeightPosition = Math.abs(this.bottomDiagFrame.getDistance(pos).getY());
      this.tankHeight = this.topDiagFrame.getDistance(this.bottomDiagFrame).getY() - 1;
      ExtendedBlock bottomDiagBlock = new ExtendedBlock(this.worldObj.getBlock(this.bottomDiagFrame.getX(), this.bottomDiagFrame.getY(), this.bottomDiagFrame.getZ()), this.worldObj.getBlockMetadata(this.bottomDiagFrame.getX(), this.bottomDiagFrame.getY(), this.bottomDiagFrame.getZ()));
      ExtendedBlock topDiagBlock = new ExtendedBlock(this.worldObj.getBlock(this.topDiagFrame.getX(), this.topDiagFrame.getY(), this.topDiagFrame.getZ()), this.worldObj.getBlockMetadata(this.topDiagFrame.getX(), this.topDiagFrame.getY(), this.topDiagFrame.getZ()));
      this.frameBurnability = bottomDiagBlock.getBlock().getFlammability(this.worldObj, this.bottomDiagFrame.getX(), this.bottomDiagFrame.getY(), this.bottomDiagFrame.getZ(), ForgeDirection.UNKNOWN);
      TileEntity tile;
      if (bottomDiagBlock.getBlock() instanceof BlockTankFrame) {
         tile = this.worldObj.getTileEntity(this.bottomDiagFrame.getX(), this.bottomDiagFrame.getY(), this.bottomDiagFrame.getZ());
         if (tile != null && tile instanceof TileEntityTankFrame) {
            bottomDiagBlock = ((TileEntityTankFrame)tile).getBlock();
         }
      }

      if (topDiagBlock.getBlock() instanceof BlockTankFrame) {
         tile = this.worldObj.getTileEntity(this.topDiagFrame.getX(), this.topDiagFrame.getY(), this.topDiagFrame.getZ());
         if (tile != null && tile instanceof TileEntityTankFrame) {
            topDiagBlock = ((TileEntityTankFrame)tile).getBlock();
         }
      }

      if (!GenericUtil.isValidTankBlock(this.worldObj, this.bottomDiagFrame, bottomDiagBlock)) {
         return false;
      } else if (!GenericUtil.areTankBlocksValid(bottomDiagBlock, topDiagBlock, this.worldObj, this.bottomDiagFrame)) {
         return false;
      } else {
         Iterator var11 = this.maps[2].entrySet().iterator();

         Entry frameCheck;
         while(var11.hasNext()) {
            frameCheck = (Entry)var11.next();
            pos = (Position3D)frameCheck.getKey();
            if (!this.worldObj.isAirBlock(pos.getX(), pos.getY(), pos.getZ()) && !((ExtendedBlock)frameCheck.getValue()).getBlock().getUnlocalizedName().equals("railcraft.residual.heat")) {
               return false;
            }
         }

         if (FancyFluidStorage.instance.INSIDE_CAPACITY) {
            this.fluidCapacity = this.maps[2].size() * this.mbPerVirtualTank;
         } else {
            this.fluidCapacity = (this.maps[0].size() + this.maps[1].size() + this.maps[2].size()) * this.mbPerVirtualTank;
         }

         var11 = this.maps[0].entrySet().iterator();

         ExtendedBlock check;
         int burnability;
         while(var11.hasNext()) {
            frameCheck = (Entry)var11.next();
            Position3D fPos = (Position3D)frameCheck.getKey();
            check = (ExtendedBlock)frameCheck.getValue();
            burnability = check.getBlock().getFlammability(this.worldObj, fPos.getX(), fPos.getY(), fPos.getZ(), ForgeDirection.UNKNOWN);
            if (burnability > this.frameBurnability) {
               this.frameBurnability = burnability;
            }

            if (check.getBlock() instanceof BlockTankFrame) {
               tile = this.worldObj.getTileEntity(fPos.getX(), fPos.getY(), fPos.getZ());
               if (tile != null && tile instanceof TileEntityTankFrame) {
                  check = ((TileEntityTankFrame)tile).getBlock();
               }
            }

            if (!GenericUtil.areTankBlocksValid(check, bottomDiagBlock, this.worldObj, fPos)) {
               return false;
            }
         }

         List valves = new ArrayList();
         Iterator var13 = this.maps[1].entrySet().iterator();

         Entry setTiles;
         while(var13.hasNext()) {
            setTiles = (Entry)var13.next();
            pos = (Position3D)setTiles.getKey();
            check = (ExtendedBlock)setTiles.getValue();
            burnability = check.getBlock().getFlammability(this.worldObj, pos.getX(), pos.getY(), pos.getZ(), ForgeDirection.UNKNOWN);
            if (burnability > this.frameBurnability) {
               this.frameBurnability = burnability;
            }

            if (!GenericUtil.areTankBlocksValid(check, bottomDiagBlock, this.worldObj, pos) && !GenericUtil.isBlockGlass(check.getBlock(), check.getMetadata())) {
               tile = this.worldObj.getTileEntity(pos.getX(), pos.getY(), pos.getZ());
               if (tile == null) {
                  return false;
               }

               if (tile instanceof TileEntityValve) {
                  TileEntityValve valve = (TileEntityValve)tile;
                  if (valve != this) {
                     if (valve.fluidStack != null) {
                        this.fluidStack = valve.fluidStack;
                        this.updateFluidTemperature();
                     }

                     valves.add(valve);
                  }
               } else if (!(tile instanceof TileEntityTankFrame)) {
                  return false;
               }
            }
         }

         if (this.fluidStack != null) {
            this.fluidStack.amount = Math.min(this.fluidStack.amount, this.fluidCapacity);
         }

         var13 = valves.iterator();

         while(var13.hasNext()) {
            TileEntityValve valve = (TileEntityValve)var13.next();
            pos = new Position3D(valve.xCoord, valve.yCoord, valve.zCoord);
            valve.valveHeightPosition = Math.abs(this.bottomDiagFrame.getDistance(pos).getY());
            valve.setMasterPos(new Position3D(this.xCoord, this.yCoord, this.zCoord));
            this.setSlaveValveInside(this.maps[2], valve);
         }

         this.isMaster = true;

         TileEntityTankFrame tankFrame;
         for(var13 = this.maps[0].entrySet().iterator(); var13.hasNext(); this.tankFrames.add(tankFrame)) {
            setTiles = (Entry)var13.next();
            pos = (Position3D)setTiles.getKey();
            if (((ExtendedBlock)setTiles.getValue()).getBlock() != FancyFluidStorage.blockTankFrame) {
               this.worldObj.setBlock(pos.getX(), pos.getY(), pos.getZ(), FancyFluidStorage.blockTankFrame, ((ExtendedBlock)setTiles.getValue()).getMetadata(), 3);
               tankFrame = (TileEntityTankFrame)this.worldObj.getTileEntity(pos.getX(), pos.getY(), pos.getZ());
               tankFrame.initialize(this, (ExtendedBlock)setTiles.getValue());
            } else {
               tankFrame = (TileEntityTankFrame)this.worldObj.getTileEntity(pos.getX(), pos.getY(), pos.getZ());
               tankFrame.setValvePos(new Position3D(this.xCoord, this.yCoord, this.zCoord));
            }
         }

         var13 = this.maps[1].entrySet().iterator();

         while(true) {
            while(true) {
               while(var13.hasNext()) {
                  setTiles = (Entry)var13.next();
                  pos = (Position3D)setTiles.getKey();
                  tile = this.worldObj.getTileEntity(pos.getX(), pos.getY(), pos.getZ());
                  if (tile != null) {
                     if (tile instanceof TileEntityValve && tile != this) {
                        this.otherValves.add((TileEntityValve)tile);
                     } else if (tile instanceof TileEntityTankFrame) {
                        ((TileEntityTankFrame)tile).setValvePos(new Position3D(this.xCoord, this.yCoord, this.zCoord));
                        this.tankFrames.add((TileEntityTankFrame)tile);
                     } else if (GenericUtil.isTileEntityAcceptable(((ExtendedBlock)setTiles.getValue()).getBlock(), tile)) {
                        this.worldObj.setBlock(pos.getX(), pos.getY(), pos.getZ(), FancyFluidStorage.blockTankFrame, ((ExtendedBlock)setTiles.getValue()).getMetadata(), 2);
                        tankFrame = (TileEntityTankFrame)this.worldObj.getTileEntity(pos.getX(), pos.getY(), pos.getZ());
                        tankFrame.initialize(this, (ExtendedBlock)setTiles.getValue());
                        this.tankFrames.add(tankFrame);
                     }
                  } else {
                     this.worldObj.setBlock(pos.getX(), pos.getY(), pos.getZ(), FancyFluidStorage.blockTankFrame, ((ExtendedBlock)setTiles.getValue()).getMetadata(), 2);
                     tankFrame = (TileEntityTankFrame)this.worldObj.getTileEntity(pos.getX(), pos.getY(), pos.getZ());
                     tankFrame.initialize(this, (ExtendedBlock)setTiles.getValue());
                     this.tankFrames.add(tankFrame);
                  }
               }

               this.isValid = true;
               return true;
            }
         }
      }
   }

   public void breakTank(TileEntity frame) {
      if (!this.worldObj.isRemote) {
         if (!this.isMaster() && this.getMaster() != null) {
            if (this.getMaster() != this) {
               this.getMaster().breakTank(frame);
            }

         } else {
            this.setValid(false);
            Iterator var2 = this.otherValves.iterator();

            while(var2.hasNext()) {
               TileEntityValve valve = (TileEntityValve)var2.next();
               valve.fluidStack = this.getFluid();
               valve.updateFluidTemperature();
               valve.master = null;
               valve.setValid(false);
               valve.updateBlockAndNeighbors();
            }

            var2 = this.tankFrames.iterator();

            while(var2.hasNext()) {
               TileEntityTankFrame tankFrame = (TileEntityTankFrame)var2.next();
               if (frame != tankFrame) {
                  tankFrame.breakFrame();
               }
            }

            this.tankFrames.clear();
            this.otherValves.clear();
            this.updateBlockAndNeighbors();
         }
      }
   }

   public void setValid(boolean isValid) {
      this.isValid = isValid;
   }

   public boolean isValid() {
      return this.getMaster() != null && this.getMaster().isValid;
   }

   public void updateBlockAndNeighbors() {
      this.updateBlockAndNeighbors(false);
   }

   private void markForUpdate(boolean onlyThis) {
      Iterator var2;
      if (!onlyThis || this.lastComparatorOut != this.getComparatorOutput()) {
         this.lastComparatorOut = this.getComparatorOutput();
         var2 = this.otherValves.iterator();

         while(var2.hasNext()) {
            TileEntityValve valve = (TileEntityValve)var2.next();
            valve.updateBlockAndNeighbors();
         }
      }

      if (!onlyThis) {
         var2 = this.tankFrames.iterator();

         while(var2.hasNext()) {
            TileEntityTankFrame frame = (TileEntityTankFrame)var2.next();
            frame.markForUpdate();
         }
      }

      this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
   }

   public void updateBlockAndNeighbors(boolean onlyThis) {
      if (!this.worldObj.isRemote) {
         this.markForUpdate(onlyThis);
         if (this.otherValves != null) {
            Iterator var2 = this.otherValves.iterator();

            while(var2.hasNext()) {
               TileEntityValve otherValve = (TileEntityValve)var2.next();
               otherValve.isValid = this.isValid;
               otherValve.markForUpdate(true);
            }
         }

         ForgeDirection outside = this.getInside().getOpposite();
         TileEntity outsideTile = this.worldObj.getTileEntity(this.xCoord + outside.offsetX, this.yCoord + outside.offsetY, this.zCoord + outside.offsetZ);
         if (outsideTile != null && FancyFluidStorage.proxy.BUILDCRAFT_LOADED && outsideTile instanceof IPipeTile) {
            ((IPipeTile)outsideTile).scheduleNeighborChange();
         }

         this.worldObj.notifyBlockChange(this.xCoord, this.yCoord, this.zCoord, FancyFluidStorage.blockValve);
         this.worldObj.markBlockForUpdate(this.xCoord + outside.offsetX, this.yCoord + outside.offsetY, this.zCoord + outside.offsetZ);
      }
   }

   public boolean isMaster() {
      return this.isMaster;
   }

   public TileEntityValve getMaster() {
      if (this.isMaster()) {
         return this;
      } else {
         if (this.masterValvePos != null) {
            TileEntity tile = this.worldObj.getTileEntity(this.masterValvePos.getX(), this.masterValvePos.getY(), this.masterValvePos.getZ());
            this.master = tile instanceof TileEntityValve ? (TileEntityValve)tile : null;
         }

         return this.master;
      }
   }

   public void setMasterPos(Position3D masterValvePos) {
      this.masterValvePos = masterValvePos;
      this.master = null;
   }

   public boolean getAutoOutput() {
      return this.isValid() && this.autoOutput;
   }

   public void setAutoOutput(boolean autoOutput) {
      this.autoOutput = autoOutput;
      this.updateBlockAndNeighbors(true);
   }

   public void readFromNBT(NBTTagCompound tag) {
      super.readFromNBT(tag);
      this.isValid = tag.getBoolean("isValid");
      this.inside = ForgeDirection.getOrientation(tag.getInteger("inside"));
      this.isMaster = tag.getBoolean("master");
      int[] bottomDiagF;
      if (this.isMaster()) {
         if (tag.getBoolean("hasFluid")) {
            if (tag.hasKey("fluidID")) {
               this.fluidStack = new FluidStack(FluidRegistry.getFluid(tag.getInteger("fluidID")), tag.getInteger("fluidAmount"));
            } else if (tag.hasKey("fluidName")) {
               this.fluidStack = new FluidStack(FluidRegistry.getFluid(tag.getString("fluidName")), tag.getInteger("fluidAmount"));
            }

            this.updateFluidTemperature();
         } else {
            this.fluidStack = null;
         }

         this.tankHeight = tag.getInteger("tankHeight");
         this.fluidCapacity = tag.getInteger("fluidCapacity");
      } else if (this.getMaster() == null && tag.hasKey("masterValve")) {
         bottomDiagF = tag.getIntArray("masterValve");
         this.setMasterPos(new Position3D(bottomDiagF[0], bottomDiagF[1], bottomDiagF[2]));
      }

      this.autoOutput = tag.getBoolean("autoOutput");
      if (tag.hasKey("valveName")) {
         this.setValveName(tag.getString("valveName"));
      } else {
         this.setValveName(GenericUtil.getUniqueValveName(this));
      }

      if (tag.hasKey("bottomDiagF")) {
         bottomDiagF = tag.getIntArray("bottomDiagF");
         int[] topDiagF = tag.getIntArray("topDiagF");
         this.bottomDiagFrame = new Position3D(bottomDiagF[0], bottomDiagF[1], bottomDiagF[2]);
         this.topDiagFrame = new Position3D(topDiagF[0], topDiagF[1], topDiagF[2]);
      }

   }

   public void writeToNBT(NBTTagCompound tag) {
      tag.setBoolean("isValid", this.isValid);
      tag.setInteger("inside", this.inside.ordinal());
      tag.setBoolean("master", this.isMaster());
      if (this.isMaster()) {
         tag.setBoolean("hasFluid", this.fluidStack != null);
         if (this.fluidStack != null) {
            tag.setString("fluidName", FluidRegistry.getFluidName(this.fluidStack));
            tag.setInteger("fluidAmount", this.fluidStack.amount);
         }

         tag.setInteger("tankHeight", this.tankHeight);
         tag.setInteger("fluidCapacity", this.fluidCapacity);
      } else if (this.getMaster() != null) {
         int[] masterPos = new int[]{this.getMaster().xCoord, this.getMaster().yCoord, this.getMaster().zCoord};
         tag.setIntArray("masterValve", masterPos);
      }

      tag.setBoolean("autoOutput", this.autoOutput);
      if (!this.getValveName().isEmpty()) {
         tag.setString("valveName", this.getValveName());
      }

      if (this.bottomDiagFrame != null && this.topDiagFrame != null) {
         tag.setIntArray("bottomDiagF", new int[]{this.bottomDiagFrame.getX(), this.bottomDiagFrame.getY(), this.bottomDiagFrame.getZ()});
         tag.setIntArray("topDiagF", new int[]{this.topDiagFrame.getX(), this.topDiagFrame.getY(), this.topDiagFrame.getZ()});
      }

      super.writeToNBT(tag);
   }

   public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
      this.readFromNBT(pkt.func_148857_g());
   }

   public Packet getDescriptionPacket() {
      NBTTagCompound tag = new NBTTagCompound();
      this.writeToNBT(tag);
      return new S35PacketUpdateTileEntity(this.xCoord, this.yCoord, this.zCoord, 0, tag);
   }

   public AxisAlignedBB getRenderBoundingBox() {
      return this.bottomDiagFrame != null && this.topDiagFrame != null ? AxisAlignedBB.getBoundingBox((double)this.bottomDiagFrame.getX(), (double)this.bottomDiagFrame.getY(), (double)this.bottomDiagFrame.getZ(), (double)this.topDiagFrame.getX(), (double)this.topDiagFrame.getY(), (double)this.topDiagFrame.getZ()) : super.getRenderBoundingBox();
   }

   public int getFluidLuminosity() {
      FluidStack fstack = this.getFluid();
      if (fstack == null) {
         return 0;
      } else {
         Fluid fluid = fstack.getFluid();
         return fluid == null ? 0 : fluid.getLuminosity(fstack);
      }
   }

   public void updateFluidTemperature() {
      FluidStack fstack = this.fluidStack;
      if (fstack != null) {
         Fluid fluid = fstack.getFluid();
         if (fluid != null) {
            this.fluidTemperature = fluid.getTemperature(fstack);
         }
      }
   }

   public FluidStack getFluid() {
      if (!this.isValid()) {
         return null;
      } else {
         return this.getMaster() == this ? this.fluidStack : this.getMaster().fluidStack;
      }
   }

   public int getFluidAmount() {
      return this.getFluid() == null ? 0 : this.getFluid().amount;
   }

   public int getCapacity() {
      if (!this.isValid()) {
         return 0;
      } else {
         return this.getMaster() == this ? this.fluidCapacity : this.getMaster().fluidCapacity;
      }
   }

   public FluidTankInfo getInfo() {
      return !this.isValid() ? null : new FluidTankInfo(this.getMaster());
   }

   public int fill(FluidStack resource, boolean doFill) {
      if (this.getMaster() != this) {
         return this.getMaster().fill(resource, doFill);
      } else if (this.isValid() && (this.fluidStack == null || this.fluidStack.isFluidEqual(resource))) {
         if (this.getFluidAmount() >= this.getCapacity()) {
            Iterator var3 = this.getAllValves().iterator();

            while(var3.hasNext()) {
               TileEntityValve valve = (TileEntityValve)var3.next();
               if (valve != this) {
                  ForgeDirection outside = valve.getInside().getOpposite();
                  TileEntity tile = this.worldObj.getTileEntity(valve.xCoord + outside.offsetX, valve.yCoord + outside.offsetY, valve.zCoord + outside.offsetZ);
                  if (tile != null && tile instanceof TileEntityValve) {
                     return ((TileEntityValve)tile).fill(this.getInside(), resource, doFill);
                  }
               }
            }
         }

         if (!doFill) {
            return this.fluidStack == null ? Math.min(this.fluidCapacity, resource.amount) : Math.min(this.fluidCapacity - this.fluidStack.amount, resource.amount);
         } else if (this.fluidStack == null) {
            this.fluidStack = new FluidStack(resource, Math.min(this.fluidCapacity, resource.amount));
            this.updateFluidTemperature();
            this.setNeedsUpdate();
            this.fluidIntake += this.fluidStack.amount;
            return this.fluidStack.amount;
         } else {
            int filled = this.fluidCapacity - this.fluidStack.amount;
            if (resource.amount < filled) {
               FluidStack var10000 = this.fluidStack;
               var10000.amount += resource.amount;
               filled = resource.amount;
            } else {
               this.fluidStack.amount = this.fluidCapacity;
            }

            this.fluidIntake += filled;
            this.getMaster().setNeedsUpdate();
            return filled;
         }
      } else {
         return 0;
      }
   }

   public FluidStack drain(int maxDrain, boolean doDrain) {
      if (this.getMaster() == this) {
         if (this.isValid() && this.fluidStack != null) {
            int drained = maxDrain;
            if (this.fluidStack.amount < maxDrain) {
               drained = this.fluidStack.amount;
            }

            FluidStack stack = new FluidStack(this.fluidStack, drained);
            if (doDrain) {
               FluidStack var10000 = this.fluidStack;
               var10000.amount -= drained;
               if (this.fluidStack.amount <= 0) {
                  this.fluidStack = null;
                  this.updateFluidTemperature();
               }

               this.fluidOuttake += drained;
               this.getMaster().setNeedsUpdate();
            }

            return stack;
         } else {
            return null;
         }
      } else {
         return this.getMaster().drain(maxDrain, doDrain);
      }
   }

   public double getFillPercentage() {
      return this.getFluid() == null ? 0.0D : Math.floor((double)this.getFluidAmount() / (double)this.getCapacity() * 100.0D);
   }

   public int fillFromContainer(ForgeDirection from, FluidStack resource, boolean doFill) {
      if (!this.canFillIncludingContainers(from, resource.getFluid())) {
         return 0;
      } else {
         return this.getMaster() == this ? this.fill(resource, doFill) : this.getMaster().fill(resource, doFill);
      }
   }

   public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
      if (!this.canFill(from, resource.getFluid())) {
         return 0;
      } else {
         return this.getMaster() == this ? this.fill(resource, doFill) : this.getMaster().fill(resource, doFill);
      }
   }

   public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {
      return this.getMaster() == this ? this.drain(resource.amount, doDrain) : this.getMaster().drain(resource.amount, doDrain);
   }

   public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
      return this.getMaster() == this ? this.drain(maxDrain, doDrain) : this.getMaster().drain(maxDrain, doDrain);
   }

   public boolean canFillIncludingContainers(ForgeDirection from, Fluid fluid) {
      if (!this.isValid()) {
         return false;
      } else if (this.getFluid() != null && this.getFluid().getFluid() != fluid) {
         return false;
      } else if (this.getFluidAmount() >= this.getCapacity()) {
         Iterator var3 = this.getAllValves().iterator();

         while(var3.hasNext()) {
            TileEntityValve valve = (TileEntityValve)var3.next();
            if (valve != this && valve.valveHeightPosition > this.getTankHeight()) {
               ForgeDirection outside = valve.getInside().getOpposite();
               TileEntity tile = this.worldObj.getTileEntity(valve.xCoord + outside.offsetX, valve.yCoord + outside.offsetY, valve.zCoord + outside.offsetZ);
               if (tile != null && tile instanceof TileEntityValve) {
                  return ((TileEntityValve)tile).canFill(valve.getInside(), fluid);
               }
            }
         }

         return false;
      } else {
         return true;
      }
   }

   public boolean canFill(ForgeDirection from, Fluid fluid) {
      if (!this.canFillIncludingContainers(from, fluid)) {
         return false;
      } else {
         return !this.getAutoOutput() || this.valveHeightPosition > this.getTankHeight() || (double)((float)this.valveHeightPosition + 0.5F) >= (double)this.getTankHeight() * this.getFillPercentage();
      }
   }

   public boolean canDrain(ForgeDirection from, Fluid fluid) {
      if (!this.isValid()) {
         return false;
      } else if (this.getFluid() == null) {
         return false;
      } else {
         return this.getFluid().getFluid() == fluid && this.getFluidAmount() > 0;
      }
   }

   public FluidTankInfo[] getTankInfo(ForgeDirection from) {
      if (!this.isValid()) {
         return null;
      } else {
         return this.getMaster() == this ? new FluidTankInfo[]{this.getInfo()} : this.getMaster().getTankInfo(from);
      }
   }

   @Method(
      modid = "BuildCraftAPI|Transport"
   )
   public ConnectOverride overridePipeConnection(PipeType pipeType, ForgeDirection from) {
      return !this.isValid() ? ConnectOverride.DISCONNECT : ConnectOverride.CONNECT;
   }

   public String[] methodNames() {
      return new String[]{"getFluidName", "getFluidAmount", "getFluidCapacity", "setAutoOutput", "doesAutoOutput"};
   }

   @Method(
      modid = "ComputerCraft"
   )
   public String getType() {
      return "ffs_valve";
   }

   @Method(
      modid = "ComputerCraft"
   )
   public String[] getMethodNames() {
      return this.methodNames();
   }

   @Method(
      modid = "ComputerCraft"
   )
   public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments) throws LuaException, InterruptedException {
      List valves;
      Iterator var7;
      TileEntityValve valve;
      switch(method) {
      case 0:
         if (this.getFluid() == null) {
            return null;
         }

         return new Object[]{this.getFluid().getLocalizedName()};
      case 1:
         return new Object[]{this.getFluidAmount()};
      case 2:
         return new Object[]{this.getCapacity()};
      case 3:
         if (arguments.length == 1) {
            if (!(arguments[0] instanceof Boolean)) {
               throw new LuaException("expected argument 1 to be of type \"boolean\", found \"" + arguments[0].getClass().getSimpleName() + "\"");
            } else {
               Iterator var10 = this.getAllValves().iterator();

               while(var10.hasNext()) {
                  valve = (TileEntityValve)var10.next();
                  valve.setAutoOutput((Boolean)arguments[0]);
               }

               return new Object[]{(Boolean)arguments[0]};
            }
         } else if (arguments.length != 2) {
            throw new LuaException("insufficient number of arguments found - expected 1 or 2, got " + arguments.length);
         } else if (!(arguments[0] instanceof String)) {
            throw new LuaException("expected argument 1 to be of type \"String\", found \"" + arguments[0].getClass().getSimpleName() + "\"");
         } else if (!(arguments[1] instanceof Boolean)) {
            throw new LuaException("expected argument 2 to be of type \"boolean\", found \"" + arguments[1].getClass().getSimpleName() + "\"");
         } else {
            valves = this.getValvesByName((String)arguments[0]);
            if (valves.isEmpty()) {
               throw new LuaException("no valves found");
            } else {
               List valveNames = new ArrayList();
               var7 = valves.iterator();

               while(var7.hasNext()) {
                  valve = (TileEntityValve)var7.next();
                  valve.setAutoOutput((Boolean)arguments[1]);
                  valveNames.add(valve.getValveName());
               }

               return new Object[]{valveNames};
            }
         }
      case 4:
         if (arguments.length == 0) {
            Map valveOutputs = new HashMap();
            Iterator var11 = this.getAllValves().iterator();

            while(var11.hasNext()) {
               valve = (TileEntityValve)var11.next();
               valveOutputs.put(valve.getValveName(), valve.getAutoOutput());
            }

            return new Object[]{valveOutputs};
         } else if (arguments.length != 1) {
            throw new LuaException("insufficient number of arguments found - expected 1, got " + arguments.length);
         } else if (!(arguments[0] instanceof String)) {
            throw new LuaException("expected argument 1 to be of type \"String\", found \"" + arguments[0].getClass().getSimpleName() + "\"");
         } else {
            valves = this.getValvesByName((String)arguments[0]);
            if (valves.isEmpty()) {
               throw new LuaException("no valves found");
            }

            Map valveOutputs = new HashMap();
            var7 = valves.iterator();

            while(var7.hasNext()) {
               valve = (TileEntityValve)var7.next();
               valveOutputs.put(valve.getValveName(), valve.getAutoOutput());
            }

            return new Object[]{valveOutputs};
         }
      default:
         return null;
      }
   }

   @Method(
      modid = "ComputerCraft"
   )
   public void attach(IComputerAccess computer) {
   }

   @Method(
      modid = "ComputerCraft"
   )
   public void detach(IComputerAccess computer) {
   }

   @Method(
      modid = "ComputerCraft"
   )
   public boolean equals(IPeripheral other) {
      return false;
   }

   @Method(
      modid = "OpenComputers"
   )
   public String getComponentName() {
      return "ffs_valve";
   }

   @Method(
      modid = "OpenComputers"
   )
   public String[] methods() {
      return this.methodNames();
   }

   @Method(
      modid = "OpenComputers"
   )
   public Object[] invoke(String method, Context context, Arguments args) throws Exception {
      byte var5 = -1;
      switch(method.hashCode()) {
      case -1180582516:
         if (method.equals("getFluidAmount")) {
            var5 = 1;
         }
         break;
      case -876829505:
         if (method.equals("getFluidName")) {
            var5 = 0;
         }
         break;
      case -360472855:
         if (method.equals("doesAutoOutput")) {
            var5 = 4;
         }
         break;
      case -19813998:
         if (method.equals("setAutoOutput")) {
            var5 = 3;
         }
         break;
      case 766903534:
         if (method.equals("getFluidCapacity")) {
            var5 = 2;
         }
      }

      List valves;
      Iterator var8;
      TileEntityValve valve;
      switch(var5) {
      case 0:
         if (this.getFluid() == null) {
            return null;
         }

         return new Object[]{this.getFluid().getLocalizedName()};
      case 1:
         return new Object[]{this.getFluidAmount()};
      case 2:
         return new Object[]{this.getCapacity()};
      case 3:
         if (args.count() == 1) {
            if (!args.isBoolean(0)) {
               throw new Exception("expected argument 1 to be of type \"boolean\", found \"" + args.checkAny(0).getClass().getSimpleName() + "\"");
            } else {
               Iterator var11 = this.getAllValves().iterator();

               while(var11.hasNext()) {
                  valve = (TileEntityValve)var11.next();
                  valve.setAutoOutput(args.checkBoolean(0));
               }

               return new Object[]{args.checkBoolean(0)};
            }
         } else if (args.count() != 2) {
            throw new Exception("insufficient number of arguments found - expected 1 or 2, got " + args.count());
         } else if (!args.isString(0)) {
            throw new Exception("expected argument 1 to be of type \"String\", found \"" + args.checkAny(0).getClass().getSimpleName() + "\"");
         } else if (!args.isBoolean(1)) {
            throw new Exception("expected argument 2 to be of type \"boolean\", found \"" + args.checkAny(1).getClass().getSimpleName() + "\"");
         } else {
            valves = this.getValvesByName(args.checkString(0));
            if (valves.isEmpty()) {
               throw new Exception("no valves found");
            } else {
               List valveNames = new ArrayList();
               var8 = valves.iterator();

               while(var8.hasNext()) {
                  valve = (TileEntityValve)var8.next();
                  valve.setAutoOutput(args.checkBoolean(1));
                  valveNames.add(valve.getValveName());
               }

               return new Object[]{valveNames};
            }
         }
      case 4:
         if (args.count() == 0) {
            Map valveOutputs = new HashMap();
            Iterator var12 = this.getAllValves().iterator();

            while(var12.hasNext()) {
               valve = (TileEntityValve)var12.next();
               valveOutputs.put(valve.getValveName(), valve.getAutoOutput());
            }

            return new Object[]{valveOutputs};
         } else if (args.count() != 1) {
            throw new Exception("insufficient number of arguments found - expected 1, got " + args.count());
         } else if (!args.isString(0)) {
            throw new Exception("expected argument 1 to be of type \"String\", found \"" + args.checkAny(0).getClass().getSimpleName() + "\"");
         } else {
            valves = this.getValvesByName(args.checkString(0));
            if (valves.isEmpty()) {
               throw new Exception("no valves found");
            }

            Map valveOutputs = new HashMap();
            var8 = valves.iterator();

            while(var8.hasNext()) {
               valve = (TileEntityValve)var8.next();
               valveOutputs.put(valve.getValveName(), valve.getAutoOutput());
            }

            return new Object[]{valveOutputs};
         }
      default:
         return null;
      }
   }

   @Method(
      modid = "funkylocomotion"
   )
   public boolean canMove(World worldObj, int x, int y, int z) {
      return false;
   }

   public int getComparatorOutput() {
      return MathHelper.floor_float((float)this.getFluidAmount() / (float)this.getCapacity() * 14.0F);
   }
}
