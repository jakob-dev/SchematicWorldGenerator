package de.jakob.worldschematics.commands.subcommands;

import de.jakob.worldschematics.WorldSchematics;
import de.jakob.worldschematics.schematics.Schematic;
import de.jakob.worldschematics.schematics.SchematicManager;
import de.jakob.worldschematics.schematics.SchematicSelection;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.List;

public class CreateCommand extends SubCommand {

    @Override
    public void onCommand(WorldSchematics plugin, CommandSender sender, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getPREFIX() + "This sub-command can only be executed by a player!");
            return;
        }

        if (!sender.hasPermission("ws.create")) {
            sender.sendMessage(plugin.getPREFIX() + "§cYou don't have permission to use this command!");
            return;
        }


        if (args.length != 2) {
            player.sendMessage(plugin.getPREFIX() + "Invalid usage. Use /ws create <name>");
            return;
        }

        String schematicName = args[1].toLowerCase();

        SchematicManager schematicManager = plugin.getSchematicManager();

        if (schematicManager.schematicExists(schematicName)) {
            player.sendMessage(plugin.getPREFIX() + "A schematic with that name already exists!");
            return;
        }

        SchematicSelection schematicSelection = schematicManager.getSchematicSelections().get(player.getUniqueId());

        if (schematicSelection == null || !schematicSelection.isValid()) {
            player.sendMessage(plugin.getPREFIX() + "You need to make a valid selection first.");
            return;
        }

        if (schematicManager.isWorking()) {
            player.sendMessage(plugin.getPREFIX() + "A schematic is already being processed at this moment.");
            return;
        }


        player.sendMessage(plugin.getPREFIX() + "Starting creation of schematic §b" + schematicName + "§7...");
        long start = System.currentTimeMillis();
        schematicManager.setWorking(true);

        List<String> includedEntityTypes = plugin.getConfig().getStringList("Saving.entity-types");
        Schematic schematic = new Schematic(schematicName, schematicSelection.getWorld(), schematicSelection.getChunk1(), schematicSelection.getChunk2(), includedEntityTypes);

        schematic.create();
        long duration = System.currentTimeMillis() - start;
        player.sendMessage(plugin.getPREFIX() + "§7Schematic §b" + schematicName + " §7has been created!");
        player.sendMessage("    §7duration: §b" + duration + " §7ms");
        player.sendMessage("    §7chunks: §b" + schematic.getTemplateSections().size());
        player.sendMessage("    §7chunk-sections: §b" + schematic.getChunkSectionCount());
        player.sendMessage("    §7entities: §b" + schematic.getEntityCount());
        player.sendMessage("    §7block-entities: §b" + schematic.getBlockEntityCount());
        schematicManager.setWorking(false);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.getSchematicManager().save(schematic);
                player.sendMessage(plugin.getPREFIX() + "Saved §b" + schematicName + " §7to disk.");
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save schematic: " + e.getMessage());
                player.sendMessage(plugin.getPREFIX() + "§cFailed to save schematic. Check console for details.");
            }
        });

    }

    @Override
    public List<String> onTabComplete(WorldSchematics plugin, CommandSender sender, String label, String[] args) {
        return List.of();
    }

}
