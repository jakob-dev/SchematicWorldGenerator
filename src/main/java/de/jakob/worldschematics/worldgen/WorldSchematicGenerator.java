package de.jakob.worldschematics.worldgen;


import de.jakob.worldschematics.WorldSchematics;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.levelgen.Heightmap;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.craftbukkit.v1_21_R7.generator.CraftChunkData;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.*;

public class WorldSchematicGenerator extends ChunkGenerator {

    private final WorldSchematics plugin;
    private final WorldSchematicSettings settings;

    private final int schematicWidth;
    private final int schematicDepth;
    private final int gridCellSizeX;
    private final int gridCellSizeZ;

    private static Field STATES_FIELD;

    static {
        try {
            STATES_FIELD = LevelChunkSection.class.getDeclaredField("states");
            STATES_FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            Bukkit.getLogger().severe("Could not find 'states' field in LevelChunkSection. World generation will fail!");
            e.printStackTrace();
        }
    }

    public WorldSchematicGenerator(WorldSchematics plugin, WorldSchematicSettings settings) {
        this.plugin = plugin;
        this.settings = settings;

        int x1 = settings.schematic().getChunk1().getX();
        int x2 = settings.schematic().getChunk2().getX();
        int z1 = settings.schematic().getChunk1().getZ();
        int z2 = settings.schematic().getChunk2().getZ();
        this.schematicWidth = Math.abs(x2 - x1) + 1;
        this.schematicDepth = Math.abs(z2 - z1) + 1;
        this.gridCellSizeX = schematicWidth + settings.spacingChunks();
        this.gridCellSizeZ = schematicDepth + settings.spacingChunks();
    }

    @Override
    public void generateSurface(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkData chunkData) {
        int offsetInGridX = Math.floorMod(chunkX, gridCellSizeX);
        int offsetInGridZ = Math.floorMod(chunkZ, gridCellSizeZ);


        if (offsetInGridX >= schematicWidth || offsetInGridZ >= schematicDepth) {
            generateSpacingLayer(chunkData);
            return;
        }


        ChunkAccess nmsChunk = ((CraftChunkData) chunkData).getHandle();
        LevelChunkSection[] targetSections = nmsChunk.getSections();

        boolean pastedAnything = false;

        for (int i = 0; i < targetSections.length; i++) {

            PalettedContainer<BlockState> templateData = settings.schematic().getSectionData(offsetInGridX, offsetInGridZ, i);

            if (templateData != null) {
                LevelChunkSection targetSection = targetSections[i];
                try {
                    if (STATES_FIELD != null) {
                        STATES_FIELD.set(targetSection, templateData.copy());

                        targetSection.recalcBlockCounts();
                        pastedAnything = true;
                    } else {
                        plugin.getLogger().warning("STATES_FIELD is null, skipping chunk section generation.");
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("Exception pasting section-data: " + e.getMessage());
                }
            }
        }

        if (pastedAnything) {
            Heightmap.primeHeightmaps(nmsChunk, EnumSet.of(
                    Heightmap.Types.MOTION_BLOCKING,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    Heightmap.Types.OCEAN_FLOOR,
                    Heightmap.Types.WORLD_SURFACE
            ));
        }
    }

    private void generateSpacingLayer(ChunkData chunkData) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                chunkData.setBlock(x, settings.spacingLayerHeight(), z, settings.spacingMaterial());
            }
        }
    }

    @Override
    public BiomeProvider getDefaultBiomeProvider(WorldInfo worldInfo) {
        return new CustomBiomeProvider();

    }

    @Override
    @Nonnull
    public List<BlockPopulator> getDefaultPopulators(World world) {
        List<BlockPopulator> populators = new ArrayList<>();
        populators.add(new WorldSchematicBlockPopulator(settings.schematic(), settings.spacingChunks()));
        return populators;
    }

    public class CustomBiomeProvider extends BiomeProvider {

        @Override
        public Biome getBiome(WorldInfo worldInfo, int i, int i1, int i2) {
            return settings.biome();
        }

        @Override
        public List<Biome> getBiomes(WorldInfo worldInfo) {
            return List.of();
        }
    }

    public WorldSchematics getPlugin() {
        return plugin;
    }
}