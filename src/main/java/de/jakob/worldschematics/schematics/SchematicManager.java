package de.jakob.worldschematics.schematics;

import de.jakob.worldschematics.WorldSchematics;
import de.jakob.worldschematics.utils.ChunkLocation;
import de.jakob.worldschematics.utils.RelativeChunkLocation;
import de.jakob.worldschematics.worldgen.WorldSchematicGenerator;
import de.jakob.worldschematics.worldgen.WorldSchematicSettings;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_21_R7.CraftServer;

import java.io.*;
import java.util.*;

public class SchematicManager {

    private final HashMap<UUID, SchematicSelection> schematicSelections;
    private final HashMap<String, Schematic> schematics;

    private final String fileExtension = ".ws";
    private final File workingDirectory;

    private boolean working;

    public SchematicManager(WorldSchematics plugin) {
        this.schematicSelections = new HashMap<>();
        this.schematics = new HashMap<>();

        this.workingDirectory = new File(plugin.getDataFolder(), "schematics");
        if (!this.workingDirectory.mkdirs()) {
            plugin.getLogger().info("Schematics directory already exists.");
        }

        this.working = false;

        FileConfiguration config = plugin.getConfig();
        ConfigurationSection worldsSection = config.getConfigurationSection("Worlds");

        if (worldsSection == null) {
            plugin.getLogger().warning("Could not load any worlds since the config-section is empty.");
            return;
        }

        for (String worldEntry : worldsSection.getKeys(false)) {

            String worldName = config.getString("Worlds." + worldEntry + ".name", "debug");
            String schematicName = config.getString("Worlds." + worldEntry + ".schematic", "debug");
            String biomeName = config.getString("Worlds." + worldEntry + ".biome", "plains");
            int spacingChunks = config.getInt("Worlds." + worldEntry + ".spacing-chunks", 0);
            String spacingMaterial = config.getString("Worlds." + worldEntry + ".spacing-material", "grass_block");
            int spacingHeight = config.getInt("Worlds." + worldEntry + ".spacing-height", 0);
            List<String> gamerules = config.getStringList("Worlds." + worldEntry + ".gamerules");

            if (!schematicExists(schematicName)) {
                plugin.getLogger().warning("Schematic " + schematicName + " does not exist for " + worldName);
                continue;
            }
            Schematic schematic;
            try {
                schematic = load(schematicName);
            } catch (IOException e) {
                plugin.getLogger().severe("There was an error trying to load the schematic " + schematicName + ":" + e.getMessage());
                continue;
            }
            WorldSchematicSettings settings = new WorldSchematicSettings(schematic, Registry.BIOME.get(NamespacedKey.minecraft(biomeName)), spacingChunks, Registry.MATERIAL.get(NamespacedKey.minecraft(spacingMaterial)), spacingHeight);
            WorldSchematicGenerator generator = new WorldSchematicGenerator(plugin, settings);
            WorldCreator creator = new WorldCreator(worldName);
            creator.type(WorldType.FLAT);
            creator.generateStructures(false);
            creator.generator(generator);
            creator.createWorld();
        }
    }


    public boolean schematicExists(String name) {
        return new File(workingDirectory, name.toLowerCase() + fileExtension).exists();
    }

    public void handleSelection(UUID player, Location vec1, Location vec2) {

        SchematicSelection schematicSelection = schematicSelections.computeIfAbsent(player, key -> new SchematicSelection(null, null));

        if (vec1 != null) schematicSelection.setVec1(vec1);
        if (vec2 != null) schematicSelection.setVec2(vec2);
    }

    public void save(Schematic schematic) throws IOException {
        File outputFile = new File(workingDirectory, schematic.getName() + fileExtension);
        save(schematic, outputFile);
    }

    public void save(Schematic schematic, File file) throws IOException {
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(file))) {

            dos.writeUTF(schematic.getName());

            dos.writeInt(schematic.getChunk1().getX());
            dos.writeInt(schematic.getChunk1().getZ());
            dos.writeInt(schematic.getChunk2().getX());
            dos.writeInt(schematic.getChunk2().getZ());

            Map<RelativeChunkLocation, Map<Integer, PalettedContainer<BlockState>>> templateSections = schematic.getTemplateSections();
            dos.writeInt(templateSections.size());

            for (Map.Entry<RelativeChunkLocation, Map<Integer, PalettedContainer<BlockState>>> entry : templateSections.entrySet()) {
                RelativeChunkLocation relLoc = entry.getKey();
                dos.writeInt(relLoc.getX());
                dos.writeInt(relLoc.getZ());

                Map<Integer, PalettedContainer<BlockState>> columnData = entry.getValue();
                dos.writeInt(columnData.size());

                for (Map.Entry<Integer, PalettedContainer<BlockState>> sectionEntry : columnData.entrySet()) {
                    dos.writeInt(sectionEntry.getKey()); // Section Y index

                    PalettedContainer<BlockState> container = sectionEntry.getValue();
                    // Serialize PalettedContainer to a byte array using FriendlyByteBuf
                    FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
                    try {
                        container.write(buffer);
                        byte[] data = new byte[buffer.readableBytes()];
                        buffer.readBytes(data);

                        dos.writeInt(data.length);
                        dos.write(data);
                    } finally {
                        buffer.release();
                    }
                }
            }

            Map<RelativeChunkLocation, List<CompoundTag>> blockEntities = schematic.getBlockEntities();
            dos.writeInt(blockEntities.size());

            for (Map.Entry<RelativeChunkLocation, List<CompoundTag>> entry : blockEntities.entrySet()) {
                RelativeChunkLocation relLoc = entry.getKey();
                dos.writeInt(relLoc.getX());
                dos.writeInt(relLoc.getZ());

                List<CompoundTag> tags = entry.getValue();
                dos.writeInt(tags.size());

                for (CompoundTag tag : tags) {
                    NbtIo.write(tag, dos);
                }
            }

            Map<RelativeChunkLocation, List<CompoundTag>> entities = schematic.getEntities();
            dos.writeInt(entities.size());

            for (Map.Entry<RelativeChunkLocation, List<CompoundTag>> entry : entities.entrySet()) {
                RelativeChunkLocation relLoc = entry.getKey();
                dos.writeInt(relLoc.getX());
                dos.writeInt(relLoc.getZ());

                List<CompoundTag> tags = entry.getValue();
                dos.writeInt(tags.size());

                for (CompoundTag tag : tags) {
                    NbtIo.write(tag, dos);
                }
            }
        }
    }

    public Schematic load(String name) throws IOException {
        File inputFile = new File(workingDirectory, name + fileExtension);
        return load(inputFile);
    }

    public Schematic load(File file) throws IOException {
        try (DataInputStream dis = new DataInputStream(new FileInputStream(file))) {

            String name = dis.readUTF();

            ChunkLocation chunk1 = new ChunkLocation(dis.readInt(), dis.readInt());
            ChunkLocation chunk2 = new ChunkLocation(dis.readInt(), dis.readInt());

            Schematic schematic = new Schematic(name, null, chunk1, chunk2, null);

            int templateSectionsSize = dis.readInt();
            if (templateSectionsSize < 0 || templateSectionsSize > 1000000)
                throw new IOException("Corrupted schematic file: invalid templateSectionsSize " + templateSectionsSize);

            for (int i = 0; i < templateSectionsSize; i++) {
                RelativeChunkLocation relLoc = new RelativeChunkLocation(dis.readInt(), dis.readInt());
                int columnDataSize = dis.readInt();
                if (columnDataSize < 0 || columnDataSize > 30)
                    throw new IOException("Corrupted schematic file: invalid columnDataSize " + columnDataSize);

                Map<Integer, PalettedContainer<BlockState>> columnData = new HashMap<>();

                for (int j = 0; j < columnDataSize; j++) {
                    int sectionY = dis.readInt();
                    int dataLength = dis.readInt();
                    byte[] data = new byte[dataLength];
                    dis.readFully(data);

                    FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
                    try {
                        MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
                        PalettedContainer<BlockState> container = PalettedContainerFactory.create(server.registryAccess()).createForBlockStates();
                        container.read(buffer);
                        columnData.put(sectionY, container);
                    } finally {
                        buffer.release();
                    }
                }
                schematic.getTemplateSections().put(relLoc, columnData);
            }

            int blockEntitiesSize = dis.readInt();
            for (int i = 0; i < blockEntitiesSize; i++) {
                RelativeChunkLocation relLoc = new RelativeChunkLocation(dis.readInt(), dis.readInt());
                int tagsSize = dis.readInt();
                List<CompoundTag> tags = new ArrayList<>(tagsSize);
                for (int j = 0; j < tagsSize; j++) {
                    tags.add(NbtIo.read(dis));
                }
                schematic.getBlockEntities().put(relLoc, tags);
            }

            int entitiesSize = dis.readInt();
            for (int i = 0; i < entitiesSize; i++) {
                RelativeChunkLocation relLoc = new RelativeChunkLocation(dis.readInt(), dis.readInt());
                int tagsSize = dis.readInt();
                List<CompoundTag> tags = new ArrayList<>(tagsSize);
                for (int j = 0; j < tagsSize; j++) {
                    tags.add(NbtIo.read(dis));
                }
                schematic.getEntities().put(relLoc, tags);
            }
            schematics.put(schematic.getName(), schematic);
            return schematic;
        }
    }

    public Schematic getSchematic(String name) {
        return schematics.get(name);
    }

    public HashMap<UUID, SchematicSelection> getSchematicSelections() {
        return schematicSelections;
    }

    public boolean isWorking() {
        return working;
    }

    public void setWorking(boolean working) {
        this.working = working;
    }

}
