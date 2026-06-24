package com.kclucas.advancedshadows.client;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.block.LeavesBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import org.joml.Matrix4f;

public class ShadowOverlayRenderer {

    private static final int RENDER_RADIUS = 16;
    private static final int RENDER_Y_RADIUS = 4;
    private static final float OVERLAY_Y_OFFSET = 0.005f;
    private static final float ALPHA = 0.6f;

    public static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        World world = client.world;
        if (world == null || client.player == null) return;

        if (!world.getDimension().hasSkyLight()) return;

        BlockPos playerPos = client.player.getBlockPos();
        Vec3d cam = client.gameRenderer.getCamera().getCameraPos();

        VertexConsumerProvider consumers = context.consumers();
        MatrixStack matrices = context.matrices();

        if (consumers == null) return;

        matrices.push();

        Matrix4f m = matrices.peek().getPositionMatrix();
        VertexConsumer fill = consumers.getBuffer(RenderLayers.debugFilledBox());

        for (int dx = -RENDER_RADIUS; dx <= RENDER_RADIUS; dx++) {
            for (int dz = -RENDER_RADIUS; dz <= RENDER_RADIUS; dz++) {
                for (int dy = -RENDER_Y_RADIUS; dy <= RENDER_Y_RADIUS; dy++) {
                    BlockPos pos = playerPos.add(dx, dy, dz);

                    // Overlay-Position muss Luft sein
                    if (!world.getBlockState(pos).isAir()) continue;
                    // Block darunter muss solid ODER Leaves sein
                    if (!isWalkableSurface(world, pos.down())) continue;

                    int skyLight = world.getLightLevel(LightType.SKY, pos);

                    float r, g, b;

                    if (skyLight == 0) {
                        r = 1.0f; g = 0.1f; b = 0.1f;       // Rot   – kein Skylight
                    } else if (skyLight <= 7) {
                        r = 1.0f; g = 0.5f; b = 0.0f;        // Orange – stark abgeschattet
                    } else if (skyLight <= 14) {
                        r = 1.0f; g = 1.0f; b = 0.0f;        // Gelb  – leicht abgeschattet
                    } else {
                        continue;                              // Volles Himmelslicht → kein Overlay
                    }

                    double x1 = pos.getX() - cam.x;
                    double y1 = pos.getY() + OVERLAY_Y_OFFSET - cam.y;
                    double x2 = x1 + 1.0;
                    double z1 = pos.getZ() - cam.z;
                    double z2 = z1 + 1.0;

                    quad(fill, m, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, r, g, b, ALPHA);
                    quad(fill, m, x1, y1, z2, x2, y1, z2, x2, y1, z1, x1, y1, z1, r, g, b, ALPHA);
                }
            }
        }

        matrices.pop();
    }

    /**
     * Gibt true zurück wenn der Block als Oberfläche für das Overlay gilt:
     * solide Blöcke (Erde, Stein, …) ODER Leaves (Baumblätter).
     */
    private static boolean isWalkableSurface(World world, BlockPos pos) {
        var state = world.getBlockState(pos);
        return state.isSolidBlock(world, pos) || state.getBlock() instanceof LeavesBlock;
    }

    private static void quad(VertexConsumer v, Matrix4f m,
                             double ax, double ay, double az,
                             double bx, double by, double bz,
                             double cx, double cy, double cz,
                             double dx, double dy, double dz,
                             float r, float g, float b, float a) {
        v.vertex(m, (float) ax, (float) ay, (float) az).color(r, g, b, a);
        v.vertex(m, (float) bx, (float) by, (float) bz).color(r, g, b, a);
        v.vertex(m, (float) cx, (float) cy, (float) cz).color(r, g, b, a);
        v.vertex(m, (float) dx, (float) dy, (float) dz).color(r, g, b, a);
    }
}