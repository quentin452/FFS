package com.lordmau5.ffs.util;

public class Position3D {
   private int x;
   private int y;
   private int z;

   public Position3D(int x, int y, int z) {
      this.x = x;
      this.y = y;
      this.z = z;
   }

   public int getX() {
      return this.x;
   }

   public int getY() {
      return this.y;
   }

   public int getZ() {
      return this.z;
   }

   public Position3D getDistance(Position3D otherPoint) {
      return new Position3D(this.getX() - otherPoint.getX(), this.getY() - otherPoint.getY(), this.getZ() - otherPoint.getZ());
   }

   public boolean equals(Object obj) {
      if (!(obj instanceof Position3D)) {
         return false;
      } else {
         Position3D other = (Position3D)obj;
         return other.getX() == this.getX() && other.getY() == this.getY() && other.getZ() == this.getZ();
      }
   }

   public String toString() {
      return "X: " + this.getX() + " - Y: " + this.getY() + " - Z: " + this.getZ();
   }

   public int hashCode() {
      int hash = 23;
      hash = hash * 31 + this.getX();
      hash = hash * 31 + this.getY();
      hash = hash * 31 + this.getZ();
      return hash;
   }
}
