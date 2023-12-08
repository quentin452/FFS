package com.lordmau5.ffs.network;

import com.lordmau5.ffs.network.handlers.server.UpdateAutoOutput_Server;
import com.lordmau5.ffs.network.handlers.server.UpdateValveName_Server;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.FMLEmbeddedChannel;
import cpw.mods.fml.common.network.FMLOutboundHandler;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.FMLOutboundHandler.OutboundTarget;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import java.util.EnumMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.Packet;

public class NetworkHandler {
   private static EnumMap channels;

   public static void registerChannels(Side side) {
      channels = NetworkRegistry.INSTANCE.newChannel("ffs", new ChannelHandler[]{new PacketCodec()});
      ChannelPipeline pipeline = ((FMLEmbeddedChannel)channels.get(Side.SERVER)).pipeline();
      String targetName = ((FMLEmbeddedChannel)channels.get(Side.SERVER)).findChannelHandlerNameForType(PacketCodec.class);
      pipeline.addAfter(targetName, "UpdateAutoOutput_Server", new UpdateAutoOutput_Server());
      pipeline.addAfter(targetName, "UpdateValveName_Server", new UpdateValveName_Server());
      if (side.isClient()) {
         registerClientHandlers();
      }

   }

   @SideOnly(Side.CLIENT)
   private static void registerClientHandlers() {
      ChannelPipeline pipeline = ((FMLEmbeddedChannel)channels.get(Side.CLIENT)).pipeline();
      String targetName = ((FMLEmbeddedChannel)channels.get(Side.CLIENT)).findChannelHandlerNameForType(PacketCodec.class);
   }

   public static Packet getProxyPacket(ffsPacket packet) {
      return ((FMLEmbeddedChannel)channels.get(FMLCommonHandler.instance().getEffectiveSide())).generatePacketFrom(packet);
   }

   public static void sendPacketToPlayer(ffsPacket packet, EntityPlayer player) {
      FMLEmbeddedChannel channel = (FMLEmbeddedChannel)channels.get(Side.SERVER);
      channel.attr(FMLOutboundHandler.FML_MESSAGETARGET).set(OutboundTarget.PLAYER);
      channel.attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(player);
      channel.writeOutbound(new Object[]{packet});
   }

   public static void sendPacketToAllPlayers(ffsPacket packet) {
      FMLEmbeddedChannel channel = (FMLEmbeddedChannel)channels.get(Side.SERVER);
      channel.attr(FMLOutboundHandler.FML_MESSAGETARGET).set(OutboundTarget.ALL);
      channel.writeOutbound(new Object[]{packet});
   }

   public static void sendPacketToServer(ffsPacket packet) {
      FMLEmbeddedChannel channel = (FMLEmbeddedChannel)channels.get(Side.CLIENT);
      channel.attr(FMLOutboundHandler.FML_MESSAGETARGET).set(OutboundTarget.TOSERVER);
      channel.writeOutbound(new Object[]{packet});
   }

   public static EntityPlayerMP getPlayer(ChannelHandlerContext ctx) {
      return ((NetHandlerPlayServer)ctx.channel().attr(NetworkRegistry.NET_HANDLER).get()).playerEntity;
   }
}
