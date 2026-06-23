package com.kclucas.advancedshadows.client.render;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ShadowCalculator {

    private static final int MAX_HEIGHT = 320;
    private static final int CHECK_RADIUS = 8; // Wie weit wir die "Decke" prüfen für Größenschätzung

    /**
     * Findet den obersten soliden Block an dieser X/Z Position (Boden).
     */
    public static BlockPos getGroundPos(World world, BlockPos origin) {
        int x = origin.getX();
        int z = origin.getZ();

        // Von Spielerhöhe aufwärts scannen ob Himmel blockiert ist
        int playerY = origin.getY();

        // Zuerst: Ist der Himmel hier überhaupt blockiert?
        boolean blocked = false;
        for (int y = playerY + 1; y <= MAX_HEIGHT; y++) {
            BlockState state = world.getBlockState(new BlockPos(x, y, z));
            if (!state.isAir()) {
                blocked = true;
                break;
            }
        }
        if (!blocked) return null;

        // Bodenposition finden (oberster solider Block auf Spielerniveau)
        for (int y = playerY; y >= world.getBottomY(); y--) {
            BlockState state = world.getBlockState(new BlockPos(x, y, z));
            if (!state.isAir() && state.isSolidBlock(world, new BlockPos(x, y, z))) {
                return new BlockPos(x, y, z);
            }
        }
        return null;
    }

    /**
     * Schätzt die Größe der schattenwerfenden Struktur.
     * Zählt benachbarte Blöcke die ebenfalls Schatten haben.
     * Größerer Wert = wahrscheinlicher eine Skybase.
     */
    public static int getShadowSize(World world, BlockPos groundPos) {
        int count = 0;
        int x = groundPos.getX();
        int z = groundPos.getZ();
        int playerY = groundPos.getY();

        for (int dx = -CHECK_RADIUS; dx <= CHECK_RADIUS; dx++) {
            for (int dz = -CHECK_RADIUS; dz <= CHECK_RADIUS; dz++) {
                boolean blocked = false;
                for (int y = playerY + 1; y <= MAX_HEIGHT; y++) {
                    BlockState state = world.getBlockState(new BlockPos(x + dx, y, z + dz));
                    if (!state.isAir()) {
                        blocked = true;
                        break;
                    }
                }
                if (blocked) count++;
            }
        }
        return count;
    }
}