package dev.ambience.util;

import dev.ambience.inject.Access;
import java.util.function.Predicate;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public final class RaytraceUtil {
   private RaytraceUtil() {
   }

   public static void a(ClientPlayerEntity var0, float var1, float var2) {
      float var3 = var0.getYaw();
      float var4 = var0.getPitch();
      var1 = GcdFix.a(var3, var1);
      var2 = GcdFix.b(var4, var2);
      b(var0, var1, var2);
   }

   public static void b(ClientPlayerEntity var0, float var1, float var2) {
      float var3 = var0.getYaw();
      float var4 = var0.getPitch();
      var0.setYaw(var1);
      var0.setPitch(var2);
      Access.setYRotO(var0, var1);
      Access.setXRotO(var0, var2);
      DebugLogger.a(var3, var4, var1, var2);
   }

   public static void c(ClientPlayerEntity var0, float var1, float var2) {
      float var3 = var0.getYaw();
      float var4 = var0.getPitch();
      var1 = GcdFix.a(var3, var1);
      var2 = GcdFix.b(var4, var2);
      Access.setYRotO(var0, var3);
      Access.setXRotO(var0, var4);
      var0.setYaw(var1);
      var0.setPitch(var2);
   }

   public static Box a(EndCrystalEntity var0) {
      Vec3d var1 = var0.getEntityPos();
      return new Box(var1.x - 1.0, var1.y, var1.z - 1.0, var1.x + 1.0, var1.y + 2.0, var1.z + 1.0);
   }

   public static Vec3d a(float var0, float var1) {
      return Vec3d.fromPolar(var0, var1);
   }

   public static BlockHitResult a(BlockPos var0, Direction var1) {
      Vec3d var2 = Vec3d.ofCenter(var0).add(var1.getOffsetX() * 0.5, var1.getOffsetY() * 0.5, var1.getOffsetZ() * 0.5);
      return new BlockHitResult(var2, var1, var0, false);
   }

   public static BlockHitResult a(World var0, Entity var1, Vec3d var2, float var3, float var4, double var5) {
      Vec3d var7 = a(var3, var4);
      Vec3d var8 = var2.add(var7.multiply(var5));
      BlockHitResult var9 = var0.raycast(new RaycastContext(var2, var8, ShapeType.OUTLINE, FluidHandling.NONE, var1));
      return var9 instanceof BlockHitResult var10 && var9.getType() == Type.BLOCK ? var10 : null;
   }

   public static EntityHitResult a(Entity var0, float var1, float var2, double var3, Predicate<Entity> var5) {
      return a(var0, var0.getEyePos(), var1, var2, var3, var5);
   }

   public static EntityHitResult a(Entity var0, Vec3d var1, float var2, float var3, double var4, Predicate<Entity> var6) {
      Vec3d var7 = a(var2, var3);
      Vec3d var8 = var1.add(var7.multiply(var4));
      return ProjectileUtil.raycast(var0, var1, var8, new Box(var1, var8).expand(1.0), var6, var4 * var4);
   }

   public static boolean a(Entity var0, float var1, float var2, BlockPos var3, double var4) {
      if (var0 != null && var3 != null) {
         Vec3d var6 = var0.getEyePos();
         Box var7 = new Box(var3);
         if (var7.contains(var6)) {
            return true;
         }

         Vec3d var8 = var6.add(a(var1, var2).multiply(var4));
         return var7.raycast(var6, var8).isPresent();
      } else {
         return false;
      }
   }

   public static boolean a(Entity var0, float var1, float var2, Entity var3, double var4) {
      if (var0 != null && var3 != null) {
         Box var6 = var3 instanceof EndCrystalEntity var7 ? a(var7) : var3.getBoundingBox();
         Vec3d var9 = var0.getEyePos();
         if (var6.contains(var9)) {
            return true;
         }

         Vec3d var8 = var9.add(a(var1, var2).multiply(var4));
         return var6.raycast(var9, var8).isPresent();
      } else {
         return false;
      }
   }

   public static HitResult a(World var0, Entity var1, float var2, float var3, double var4, double var6) {
      return a(var0, var1, var1.getEyePos(), var2, var3, var4, var6);
   }

   public static HitResult a(World var0, Entity var1, Vec3d var2, float var3, float var4, double var5, double var7) {
      BlockHitResult var9 = a(var0, var1, var2, var3, var4, var5);
      EntityHitResult var10 = a(var1, var2, var3, var4, var7, Entity::canHit);
      if (var9 != null && var10 != null) {
         double var11 = var9.getPos().squaredDistanceTo(var2);
         double var13 = var10.getPos().squaredDistanceTo(var2);
         return (HitResult)(var11 < var13 ? var9 : var10);
      } else {
         return (HitResult)(var9 != null ? var9 : var10);
      }
   }
}
