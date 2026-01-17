package de.jakob.worldschematics.schematics;

import de.jakob.worldschematics.utils.ChunkLocation;
import de.jakob.worldschematics.utils.RelativeChunkLocation;
import net.minecraft.nbt.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.storage.TagValueOutput;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_21_R7.CraftChunk;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftEntity;
import org.bukkit.entity.EntityType;

import java.util.*;

public class Schematic {

    private final String name;
    private final World world;
    private final ChunkLocation chunk1;
    private final ChunkLocation chunk2;
    private final List<String> includedEntityTypes;

    private final HashMap<RelativeChunkLocation, Map<Integer, PalettedContainer<BlockState>>> templateSections;
    private final HashMap<RelativeChunkLocation, List<CompoundTag>> blockEntities;
    private final HashMap<RelativeChunkLocation, List<CompoundTag>> entities;

    private int chunkSectionCount;
    private int blockEntityCount;
    private int entityCount;


    public Schematic(String name, World world, ChunkLocation chunk1, ChunkLocation chunk2, List<String> includedEntityTypes) {
        this.name = name;
        this.world = world;
        this.chunk1 = chunk1;
        this.chunk2 = chunk2;
        this.includedEntityTypes = includedEntityTypes;
        this.templateSections = new HashMap<>();
        this.blockEntities = new HashMap<>();
        this.entities = new HashMap<>();
        this.chunkSectionCount = 0;
        this.blockEntityCount = 0;
        this.entityCount = 0;
    }

    public void create() {
        int minX = Math.min(chunk1.getX(), chunk2.getX());
        int maxX = Math.max(chunk1.getX(), chunk2.getX());
        int minZ = Math.min(chunk1.getZ(), chunk2.getZ());
        int maxZ = Math.max(chunk1.getZ(), chunk2.getZ());

        int worldMinY = world.getMinHeight();
        int baseSectionIndex = worldMinY >> 4;

        int baseX = minX * 16;
        int baseY = baseSectionIndex * 16;
        int baseZ = minZ * 16;

        ChunkLocation baseLocation = new ChunkLocation(minX, minZ);

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {


                Chunk currentChunk = world.getChunkAt(x, z);
                CraftChunk craftChunk = (CraftChunk) currentChunk;
                ChunkAccess chunkAccess = craftChunk.getHandle(ChunkStatus.FULL);

                RelativeChunkLocation relativeChunkLocation = new RelativeChunkLocation(baseLocation, x, z);

                if (chunkAccess instanceof LevelChunk levelChunk) {

                    LevelChunkSection[] sections = levelChunk.getSections();
                    Map<Integer, PalettedContainer<BlockState>> chunkColumnData = new HashMap<>();

                    for (int i = 0; i < sections.length; i++) {
                        LevelChunkSection section = sections[i];
                        if (section == null || section.hasOnlyAir()) continue;

                        chunkColumnData.put(i, section.getStates().copy());
                        chunkSectionCount++;
                    }

                    if (!chunkColumnData.isEmpty()) {
                        templateSections.put(relativeChunkLocation, chunkColumnData);
                    }

                    List<CompoundTag> blockEntityList = new ArrayList<>();
                    levelChunk.getBlockEntities().values().forEach(blockEntity -> {
                        TagValueOutput tagOutput = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
                        blockEntity.saveWithFullMetadata(tagOutput);
                        CompoundTag tag = tagOutput.buildResult();

                        net.minecraft.core.BlockPos pos = blockEntity.getBlockPos();
                        int relX = pos.getX() - baseX;
                        int relY = pos.getY() - baseY;
                        int relZ = pos.getZ() - baseZ;

                        tag.putInt("x", relX);
                        tag.putInt("y", relY);
                        tag.putInt("z", relZ);

                        blockEntityList.add(tag);
                        blockEntityCount++;
                    });

                    if (!blockEntityList.isEmpty()) {
                        blockEntities.put(relativeChunkLocation, blockEntityList);
                    }

                    List<CompoundTag> entityList = new ArrayList<>();
                    for (org.bukkit.entity.Entity bukkitEntity : craftChunk.getEntities()) {
                        net.minecraft.world.entity.Entity nmsEntity = ((CraftEntity) bukkitEntity).getHandle();
                        TagValueOutput tagOutput = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
                        nmsEntity.save(tagOutput);
                        CompoundTag tag = tagOutput.buildResult();

                        EntityType entityType = bukkitEntity.getType();

                        if (!includedEntityTypes.contains(entityType.toString())) {
                            continue;
                        }

                        double relX = nmsEntity.getX() - baseX;
                        double relY = nmsEntity.getY() - baseY;
                        double relZ = nmsEntity.getZ() - baseZ;

                        ListTag newPosList = new ListTag();
                        newPosList.add(DoubleTag.valueOf(relX));
                        newPosList.add(DoubleTag.valueOf(relY));
                        newPosList.add(DoubleTag.valueOf(relZ));


                        tag.put("Pos", newPosList);
                        tag.remove("UUID");

                        entityCount++;
                        entityList.add(tag);
                    }

                    if (!entityList.isEmpty()) {
                        entities.put(relativeChunkLocation, entityList);
                    }

                }
            }
        }
    }


    public PalettedContainer<BlockState> getSectionData(int relX, int relZ, int relativeSectionY) {
        RelativeChunkLocation key = new RelativeChunkLocation(relX, relZ);
        Map<Integer, PalettedContainer<BlockState>> column = templateSections.get(key);
        if (column == null) return null;
        return column.get(relativeSectionY);
    }

    public int getChunkSectionCount() {
        return chunkSectionCount;
    }

    public int getBlockEntityCount() {
        return blockEntityCount;
    }

    public int getEntityCount() {
        return entityCount;
    }

    public String getName() {
        return name;
    }

    public ChunkLocation getChunk1() {
        return chunk1;
    }

    public ChunkLocation getChunk2() {
        return chunk2;
    }

    public HashMap<RelativeChunkLocation, Map<Integer, PalettedContainer<BlockState>>> getTemplateSections() {
        return templateSections;
    }

    public HashMap<RelativeChunkLocation, List<CompoundTag>> getEntities() {
        return entities;
    }

    public HashMap<RelativeChunkLocation, List<CompoundTag>> getBlockEntities() {
        return blockEntities;
    }

    public World getWorld() {
        return world;
    }

    public List<String> getIncludedEntityTypes() {
        return includedEntityTypes;
    }
}