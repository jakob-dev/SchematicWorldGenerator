package de.jakob.worldschematics.utils;

import org.bukkit.Location;

import java.util.Objects;

public class RelativeLocation {


    private final int x;
    private final int y;
    private final int z;

    public RelativeLocation(Location base, int absoluteX, int absoluteY, int absoluteZ) {
        this.x = absoluteX - base.getBlockX();
        this.y = absoluteY - base.getBlockY();
        this.z = absoluteZ - base.getBlockZ();
    }

    public RelativeLocation(int relativeX, int relativeY, int relativeZ) {
        this.x = relativeX;
        this.y = relativeY;
        this.z = relativeZ;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public Location getAbsoluteLocation(Location base) {
        return new Location(base.getWorld(), base.getX() + this.x, base.getY() + this.y, base.getZ() + this.z);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RelativeLocation that = (RelativeLocation) o;
        return x == that.x && y == that.y && z == that.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

}
