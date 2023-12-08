package com.lordmau5.ffs.util;

import net.minecraft.block.Block;

public class ExtendedBlock {
   private final Block block;
   private final int metadata;

   public ExtendedBlock(Block block, int metadata) {
      this.block = block;
      this.metadata = metadata;
   }

   public Block getBlock() {
      return this.block;
   }

   public int getMetadata() {
      return this.metadata;
   }

   public boolean equals(Object obj) {
      if (!(obj instanceof ExtendedBlock)) {
         return false;
      } else {
         ExtendedBlock other = (ExtendedBlock)obj;
         return other.getBlock().getUnlocalizedName().equals(this.getBlock().getUnlocalizedName()) && other.getMetadata() == this.getMetadata();
      }
   }

   public boolean equalsIgnoreMetadata(ExtendedBlock block) {
      return block.getBlock().getUnlocalizedName().equals(this.getBlock().getUnlocalizedName());
   }

   public String toString() {
      return this.getBlock().getUnlocalizedName() + ":" + this.getMetadata();
   }
}
