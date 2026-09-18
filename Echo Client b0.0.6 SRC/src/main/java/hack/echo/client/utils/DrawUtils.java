package hack.echo.client.utils;

import hack.echo.client.render3.api.FramebufferTarget;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class DrawUtils implements Imports {

    private static final double TAU = Math.PI * 2.0;

    public static void drawOutlinedCircle(
        FramebufferTarget draw,
        Vec3 center,
        Vec3 normal,
        double radius,
        int segments,
        int color
    ) {
        if (draw == null || center == null || normal == null || segments < 3 || radius <= 0.0) return;

        Vec3 n = normal.normalize();
        if (n.lengthSqr() < 1.0E-6) {
            n = new Vec3(0.0, 1.0, 0.0);
        }

        Vec3 reference = Math.abs(n.y) < 0.99 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
        Vec3 tangent = n.cross(reference).normalize();
        if (tangent.lengthSqr() < 1.0E-6) {
            tangent = new Vec3(1.0, 0.0, 0.0);
        }
        Vec3 bitangent = n.cross(tangent).normalize();

        for (int i = 0; i < segments; i++) {
            double angleA = TAU * i / segments;
            double angleB = TAU * (i + 1) / segments;

            Vec3 from = center
                .add(tangent.scale(Math.cos(angleA) * radius))
                .add(bitangent.scale(Math.sin(angleA) * radius));
            Vec3 to = center
                .add(tangent.scale(Math.cos(angleB) * radius))
                .add(bitangent.scale(Math.sin(angleB) * radius));

            draw.line(from.x, from.y, from.z, to.x, to.y, to.z, color);
        }
    }

    public static void drawBoxOutline(FramebufferTarget draw, AABB box, int color) {
        double minX = box.minX;
        double minY = box.minY;
        double minZ = box.minZ;
        double maxX = box.maxX;
        double maxY = box.maxY;
        double maxZ = box.maxZ;

        draw.line(minX, minY, minZ, maxX, minY, minZ, color);
        draw.line(maxX, minY, minZ, maxX, minY, maxZ, color);
        draw.line(maxX, minY, maxZ, minX, minY, maxZ, color);
        draw.line(minX, minY, maxZ, minX, minY, minZ, color);

        draw.line(minX, maxY, minZ, maxX, maxY, minZ, color);
        draw.line(maxX, maxY, minZ, maxX, maxY, maxZ, color);
        draw.line(maxX, maxY, maxZ, minX, maxY, maxZ, color);
        draw.line(minX, maxY, maxZ, minX, maxY, minZ, color);

        draw.line(minX, minY, minZ, minX, maxY, minZ, color);
        draw.line(maxX, minY, minZ, maxX, maxY, minZ, color);
        draw.line(maxX, minY, maxZ, maxX, maxY, maxZ, color);
        draw.line(minX, minY, maxZ, minX, maxY, maxZ, color);
    }
}
