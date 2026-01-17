package de.jakob.worldschematics.utils;

import java.util.Objects;

public class RelativeChunkLocation {

    private final int x;
    private final int z;

    public RelativeChunkLocation(ChunkLocation base, int absoluteX, int absoluteZ) {
        this.x = absoluteX - base.getX();
        this.z = absoluteZ - base.getZ();
    }

    public RelativeChunkLocation(int relativeX, int relativeZ) {
        this.x = relativeX;
        this.z = relativeZ;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    public ChunkLocation getAbsoluteLocation(ChunkLocation base) {
        return new ChunkLocation(base.getX() + this.x, base.getZ() + this.z);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RelativeChunkLocation that = (RelativeChunkLocation) o;
        return x == that.x && z == that.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, z);
    }
}