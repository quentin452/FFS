package com.lordmau5.ffs.network;

import com.lordmau5.ffs.tile.TileEntityValve;
import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;

public abstract class ffsPacket {
   public abstract void encode(ByteBuf var1);

   public abstract void decode(ByteBuf var1);

   public static class Server {
      public static class UpdateValveName extends ffsPacket {
         public int x;
         public int y;
         public int z;
         public String name;

         public UpdateValveName() {
         }

         public UpdateValveName(TileEntityValve valve, String name) {
            this.x = valve.xCoord;
            this.y = valve.yCoord;
            this.z = valve.zCoord;
            this.name = name;
         }

         public UpdateValveName(int x, int y, int z, String name) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.name = name;
         }

         public void encode(ByteBuf buffer) {
            buffer.writeInt(this.x);
            buffer.writeInt(this.y);
            buffer.writeInt(this.z);
            ByteBufUtils.writeUTF8String(buffer, this.name);
         }

         public void decode(ByteBuf buffer) {
            this.x = buffer.readInt();
            this.y = buffer.readInt();
            this.z = buffer.readInt();
            this.name = ByteBufUtils.readUTF8String(buffer);
         }
      }

      public static class UpdateAutoOutput extends ffsPacket {
         public int x;
         public int y;
         public int z;
         public boolean autoOutput;

         public UpdateAutoOutput() {
         }

         public UpdateAutoOutput(TileEntityValve valve, boolean autoOutput) {
            this.x = valve.xCoord;
            this.y = valve.yCoord;
            this.z = valve.zCoord;
            this.autoOutput = autoOutput;
         }

         public UpdateAutoOutput(int x, int y, int z, boolean autoOutput) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.autoOutput = autoOutput;
         }

         public void encode(ByteBuf buffer) {
            buffer.writeInt(this.x);
            buffer.writeInt(this.y);
            buffer.writeInt(this.z);
            buffer.writeBoolean(this.autoOutput);
         }

         public void decode(ByteBuf buffer) {
            this.x = buffer.readInt();
            this.y = buffer.readInt();
            this.z = buffer.readInt();
            this.autoOutput = buffer.readBoolean();
         }
      }
   }

   public abstract static class Client {
   }
}
