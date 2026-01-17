package de.jakob.worldschematics.utils;

public class ChunkLocation {

    private final int x;
    private final int z;

    public ChunkLocation(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

}
