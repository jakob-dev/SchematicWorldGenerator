package de.jakob.worldschematics.worldgen;

import de.jakob.worldschematics.schematics.Schematic;
import org.bukkit.Material;
import org.bukkit.block.Biome;

public record WorldSchematicSettings(Schematic schematic,
                                     Biome biome,
                                     int spacingChunks,
                                     Material spacingMaterial,
                                     int spacingLayerHeight) {
}
