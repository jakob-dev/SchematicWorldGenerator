package de.jakob.worldschematics.worldgen;


import de.jakob.worldschematics.schematics.Schematic;
import de.jakob.worldschematics.utils.RelativeChunkLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.DoubleTag;

import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.v1_21_R7.CraftRegionAccessor;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class WorldSchematicBlockPopulator extends BlockPopulator {

    private final Schematic schematic;
    private final int schematicWidth;
    private final int schematicDepth;
    private final int gridCellSizeX;
    private final int gridCellSizeZ;

    public WorldSchematicBlockPopulator(Schematic schematic, int spacingChunks) {
        this.schematic = schematic;

        int x1 = schematic.getChunk1().getX();
        int x2 = schematic.getChunk2().getX();
        int z1 = schematic.getChunk1().getZ();
        int z2 = schematic.getChunk2().getZ();
        this.schematicWidth = Math.abs(x2 - x1) + 1;
        this.schematicDepth = Math.abs(z2 - z1) + 1;
        this.gridCellSizeX = schematicWidth + spacingChunks;
        this.gridCellSizeZ = schematicDepth + spacingChunks;
    }

    @Override
    public void populate(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, LimitedRegion limitedRegion) {
        int offsetInGridX = Math.floorMod(chunkX, gridCellSizeX);
        int offsetInGridZ = Math.floorMod(chunkZ, gridCellSizeZ);

        if (offsetInGridX >= schematicWidth || offsetInGridZ >= schematicDepth) {
            return;
        }

        WorldGenLevel region = ((CraftRegionAccessor) limitedRegion).getHandle();
        net.minecraft.world.level.chunk.ChunkAccess chunk = region.getChunk(chunkX, chunkZ);

        WorldGenRegion worldGenRegion = (WorldGenRegion) region;

        int schematicInstanceOriginX = (chunkX - offsetInGridX) * 16;
        int schematicInstanceOriginZ = (chunkZ - offsetInGridZ) * 16;
        int worldMinY = worldInfo.getMinHeight();

        RelativeChunkLocation key = new RelativeChunkLocation(offsetInGridX, offsetInGridZ);


        HashMap<RelativeChunkLocation, List<CompoundTag>> blockEntities = schematic.getBlockEntities();
        List<CompoundTag> blockEntityTags = blockEntities.get(key);

        if (blockEntityTags != null && !blockEntityTags.isEmpty()) {
            for (CompoundTag tag : blockEntityTags) {
                try {
                    CompoundTag pasteTag = tag.copy();

                    int relX = pasteTag.getIntOr("x", 0);
                    int relY = pasteTag.getIntOr("y", 0);
                    int relZ = pasteTag.getIntOr("z", 0);

                    int finalX = schematicInstanceOriginX + relX;
                    int finalY = worldMinY + relY;
                    int finalZ = schematicInstanceOriginZ + relZ;

                    if ((finalX >> 4) != chunkX || (finalZ >> 4) != chunkZ) continue;

                    pasteTag.putInt("x", finalX);
                    pasteTag.putInt("y", finalY);
                    pasteTag.putInt("z", finalZ);

                    BlockPos pos = new BlockPos(finalX, finalY, finalZ);
                    net.minecraft.world.level.block.state.BlockState state = chunk.getBlockState(pos);

                    if (state.hasBlockEntity()) {
                        BlockEntity tileEntity = ((net.minecraft.world.level.block.EntityBlock) state.getBlock()).newBlockEntity(pos, state);

                        if (tileEntity != null) {
                            chunk.setBlockEntity(tileEntity);

                            ValueInput input = TagValueInput.create(
                                    ProblemReporter.DISCARDING,
                                    region.registryAccess(),
                                    pasteTag
                            );
                            tileEntity.loadWithComponents(input);
                        }
                    }
                } catch (Exception e) {
                    org.bukkit.Bukkit.getLogger().severe("Error populating block entities: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        HashMap<RelativeChunkLocation, List<CompoundTag>> entities = schematic.getEntities();
        List<CompoundTag> entityTags = entities.get(key);

        if (entityTags != null && !entityTags.isEmpty()) {
            for (CompoundTag entityTag : entityTags) {
                try {
                    CompoundTag pasteTag = entityTag.copy();

                    ListTag posList = pasteTag.getList("Pos").get(); // 6 = Double
                    if (posList.size() < 3) continue;

                    double relX = posList.getDoubleOr(0, 0D);
                    double relY = posList.getDoubleOr(1, 0D);
                    double relZ = posList.getDoubleOr(2, 0D);

                    double finalX = schematicInstanceOriginX + relX;
                    double finalY = worldMinY + relY;
                    double finalZ = schematicInstanceOriginZ + relZ;

                    ListTag newPosList = new ListTag();
                    newPosList.add(DoubleTag.valueOf(finalX));
                    newPosList.add(DoubleTag.valueOf(finalY));
                    newPosList.add(DoubleTag.valueOf(finalZ));
                    pasteTag.put("Pos", newPosList);

                    if (pasteTag.contains("block_pos")) {
                        ListTag newBlockPosList = new ListTag();
                        newBlockPosList.add(DoubleTag.valueOf(finalX));
                        newBlockPosList.add(DoubleTag.valueOf(finalY));
                        newBlockPosList.add(DoubleTag.valueOf(finalZ));
                        pasteTag.put("block_pos", newBlockPosList);
                    }

                    ValueInput input = TagValueInput.create(
                            ProblemReporter.DISCARDING,
                            region.registryAccess(),
                            pasteTag
                    );

                    EntityType.by(input).ifPresent(type -> {
                        net.minecraft.world.entity.Entity entity = type.create(region.getLevel(), EntitySpawnReason.CHUNK_GENERATION);
                        if (entity != null) {

                            entity.load(input);
                            entity.moveOrInterpolateTo(new Vec3(finalX, finalY, finalZ), entity.getYRot(), entity.getXRot());
                            worldGenRegion.addFreshEntity(entity);
                        }
                    });

                } catch (Exception e) {
                    org.bukkit.Bukkit.getLogger().severe("Error populating block entities: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
}