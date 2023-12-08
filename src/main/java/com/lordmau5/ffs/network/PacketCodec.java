package com.lordmau5.ffs.network;

import cpw.mods.fml.common.network.FMLIndexedMessageToMessageCodec;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public class PacketCodec extends FMLIndexedMessageToMessageCodec<ffsPacket> {
   int lastDiscriminator = 0;

   public PacketCodec() {
      this.addPacket(ffsPacket.Server.UpdateAutoOutput.class);
      this.addPacket(ffsPacket.Server.UpdateValveName.class);
   }

   void addPacket(Class type) {
      this.addDiscriminator(this.lastDiscriminator, type);
      ++this.lastDiscriminator;
   }

   public void encodeInto(ChannelHandlerContext ctx, ffsPacket msg, ByteBuf target) throws Exception {
      msg.encode(target);
   }

   public void decodeInto(ChannelHandlerContext ctx, ByteBuf source, ffsPacket msg) {
      msg.decode(source);
   }
}
