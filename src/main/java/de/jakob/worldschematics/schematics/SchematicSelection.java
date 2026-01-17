package de.jakob.worldschematics.schematics;

import de.jakob.worldschematics.utils.ChunkLocation;
import org.bukkit.Location;
import org.bukkit.World;

public class SchematicSelection {

    private Location vec1;
    private Location vec2;

    public SchematicSelection(Location vec1, Location vec2) {
        this.vec1 = vec1;
        this.vec2 = vec2;
    }

    public Location getVec1() {
        return vec1;
    }

    public Location getVec2() {
        return vec2;
    }

    public void setVec1(Location vec1) {
        this.vec1 = vec1;
    }

    public void setVec2(Location vec2) {
        this.vec2 = vec2;
    }

    public boolean isValid() {

        if (vec1 == null || vec2 == null) {
            return false;
        }

        return vec1.getWorld() == vec2.getWorld();
    }

    public ChunkLocation getChunk1() {
        return new ChunkLocation(vec1.getBlockX() >> 4, vec1.getBlockZ() >> 4);

    }

    public ChunkLocation getChunk2() {
        return new ChunkLocation(vec2.getBlockX() >> 4, vec2.getBlockZ() >> 4);
    }

    public World getWorld() {
        return vec1.getWorld();
    }

}
