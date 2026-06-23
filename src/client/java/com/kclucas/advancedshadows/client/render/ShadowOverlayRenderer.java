package com.kclucas.advancedshadows.client.render;

import com.kclucas.advancedshadows.client.AdvancedShadowsClient;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat.Builder
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public class ShadowOverlayRenderer {

    private static final int SCAN_RADIUS = 48;

    private static final RenderPipeline SHADOW_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(ResourceLocation.fromNamespaceAndPath("advancedshadows", "pipeline/shadow_overlay"))
                    .withDepthStencilState(Optional.empty())
                    .build()
    );

    private static final ByteBufferBuilder ALLOCATOR = new ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE);
    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();

    private static List<ShadowTile> extractedTiles = new ArrayList<>();

    private BufferBuilder buffer;
    private MappableRingBuffer vertexBuffer;

    private static ShadowOverlayRenderer INSTANCE;

    public static ShadowOverlayRenderer getInstance() {
        return INSTANCE;
    }

    public static void register() {
        INSTANCE = new ShadowOverlayRenderer();
        LevelRenderEvents.END_EXTRACTION.register(INSTANCE::extract);
        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(INSTANCE::renderAndDraw);
    }

    private void extract(LevelExtractionContext context) {
        if (!AdvancedShadowsClient.overlayEnabled) {
            extractedTiles = new ArrayList<>();
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        World world = client.level;
        BlockPos playerPos = client.player.blockPosition();
        List<ShadowTile> tiles = new ArrayList<>();

        for (int x = -SCAN_RADIUS; x <= SCAN_RADIUS; x++) {
            for (int z = -SCAN_RADIUS; z <= SCAN_RADIUS; z++) {
                BlockPos groundPos = ShadowCalculator.getGroundPos(world, playerPos.offset(x, 0, z));
                if (groundPos == null) continue;
                int size = ShadowCalculator.getShadowSize(world, groundPos);
                if (size <= 0) continue;
                float[] color = getColor(size);
                tiles.add(new ShadowTile(groundPos.getX(), groundPos.getY(), groundPos.getZ(), color[0], color[1], color[2]));
            }
        }

        extractedTiles = tiles;
    }

    private void renderAndDraw(LevelRenderContext context) {
        if (extractedTiles.isEmpty()) return;

        PoseStack matrices = context.poseStack();
        Vec3 camera = context.levelState().cameraRenderState.pos;

        matrices.pushPose();
        matrices.translate(-camera.x, -camera.y, -camera.z);

        if (this.buffer == null) {
            this.buffer = new BufferBuilder(ALLOCATOR, SHADOW_PIPELINE.getVertexFormatMode(), SHADOW_PIPELINE.getVertexFormat());
        }

        for (ShadowTile tile : extractedTiles) {
            renderQuad(matrices.last().pose(), this.buffer,
                    tile.x, tile.y + 1.01f, tile.z,
                    tile.x + 1, tile.y + 1.01f, tile.z + 1,
                    tile.r, tile.g, tile.b, 0.45f);
        }

        matrices.popPose();

        drawBuffer(Minecraft.getInstance());
    }

    private void renderQuad(Matrix4fc matrix, BufferBuilder buffer,
                            float minX, float minY, float minZ,
                            float maxX, float maxY, float maxZ,
                            float r, float g, float b, float a) {
        buffer.addVertex(matrix, minX, minY, minZ).setColor(r, g, b, a);
        buffer.addVertex(matrix, maxX, minY, minZ).setColor(r, g, b, a);
        buffer.addVertex(matrix, maxX, minY, maxZ).setColor(r, g, b, a);
        buffer.addVertex(matrix, minX, minY, maxZ).setColor(r, g, b, a);
    }

    private void drawBuffer(Minecraft client) {
        MeshData built = this.buffer.buildOrThrow();
        MeshData.DrawState drawParams = built.drawState();
        VertexFormat format = drawParams.format();

        int vertexBufferSize = drawParams.vertexCount() * format.getVertexSize();
        if (this.vertexBuffer == null || this.vertexBuffer.size() < vertexBufferSize) {
            if (this.vertexBuffer != null) this.vertexBuffer.close();
            this.vertexBuffer = new MappableRingBuffer(
                    () -> "advancedshadows shadow overlay",
                    GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE,
                    vertexBufferSize
            );
        }

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        try (GpuBuffer.MappedView view = encoder.mapBuffer(
                this.vertexBuffer.currentBuffer().slice(0, built.vertexBuffer().remaining()), false, true)) {
            MemoryUtil.memCopy(built.vertexBuffer(), view.data());
        }

        GpuBuffer vertices = this.vertexBuffer.currentBuffer();
        RenderSystem.AutoStorageIndexBuffer shapeIndex = RenderSystem.getSequentialBuffer(SHADOW_PIPELINE.getVertexFormatMode());
        GpuBuffer indices = shapeIndex.getBuffer(drawParams.indexCount());

        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .writeTransform(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);

        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
                .createRenderPass(() -> "advancedshadows shadow overlay render",
                        client.getMainRenderTarget().getColorTextureView(),
                        OptionalInt.empty(),
                        client.getMainRenderTarget().getDepthTextureView(),
                        OptionalDouble.empty())) {
            pass.setPipeline(SHADOW_PIPELINE);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", dynamicTransforms);
            pass.setVertexBuffer(0, vertices);
            pass.setIndexBuffer(indices, shapeIndex.type());
            pass.drawIndexed(0, 0, drawParams.indexCount(), 1);
        }

        built.close();
        this.vertexBuffer.rotate();
        this.buffer = null;
    }

    private static float[] getColor(int size) {
        if (size >= 16) return new float[]{1.0f, 0.1f, 0.1f};      // Rot - Skybase
        if (size >= 8)  return new float[]{1.0f, 0.55f, 0.0f};     // Orange
        return new float[]{1.0f, 1.0f, 0.0f};                       // Gelb - Baum
    }

    public void close() {
        ALLOCATOR.close();
        if (this.vertexBuffer != null) {
            this.vertexBuffer.close();
            this.vertexBuffer = null;
        }
    }

    private record ShadowTile(int x, int y, int z, float r, float g, float b) {}
}