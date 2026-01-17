package de.jakob.worldschematics.commands;

import de.jakob.worldschematics.WorldSchematics;
import de.jakob.worldschematics.commands.subcommands.CreateCommand;
import de.jakob.worldschematics.commands.subcommands.SubCommand;
import de.jakob.worldschematics.commands.subcommands.TeleportCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorldSchematicCommand implements TabExecutor {


    private final WorldSchematics plugin;
    private final Map<String, SubCommand> subCommands = new HashMap<>();

    public WorldSchematicCommand(WorldSchematics plugin) {
        this.plugin = plugin;
        subCommands.put("create", new CreateCommand());
        subCommands.put("teleport", new TeleportCommand());
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {


        if (!sender.hasPermission("ws.manage")) {
            sender.sendMessage(plugin.getPREFIX() + "§7You §cdon't §7have permission to execute this command!");
            return false;
        }

        if (args.length == 0) {

            sender.sendMessage("§7Running WorldSchematics Version: §b" + plugin.getDescription().getVersion());
            sender.sendMessage("§7Available sub-commands: §b" + subCommands.keySet());
            return true;
        }


        if (!subCommands.containsKey(args[0].toLowerCase())) {
            sender.sendMessage(plugin.getPREFIX() + "This subcommand §cdoesn't §7exist!");
            return false;
        }

        subCommands.get(args[0]).onCommand(plugin, sender, label, args);

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return new ArrayList<>(subCommands.keySet());
        }

        if (args.length > 1) {
            return subCommands.get(args[0]).onTabComplete(plugin, sender, label, args);
        }
        return null;
    }
}