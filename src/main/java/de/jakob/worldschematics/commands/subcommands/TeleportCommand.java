package de.jakob.worldschematics.commands.subcommands;

import de.jakob.worldschematics.WorldSchematics;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class TeleportCommand extends SubCommand {

    @Override
    public void onCommand(WorldSchematics plugin, CommandSender sender, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getPREFIX() + "This sub-command can only be executed by a player!");
            return;
        }

        if (!sender.hasPermission("ws.teleport")) {
            sender.sendMessage(plugin.getPREFIX() + "§cYou don't have permission to use this command!");
            return;
        }

        if (args.length != 2) {
            player.sendMessage(plugin.getPREFIX() + "Invalid usage. Use /ws teleport <world>");
            return;
        }

        String worldName = args[1];
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            player.sendMessage(plugin.getPREFIX() + "This world doesn't exist!");
            return;
        }

        player.teleport(world.getSpawnLocation());

        player.sendMessage(plugin.getPREFIX() + "You were teleported to world §b" + worldName + "§7.");


    }

    @Override
    public List<String> onTabComplete(WorldSchematics plugin, CommandSender sender, String label, String[] args) {
        if (args.length == 2)
            return new ArrayList<>(Bukkit.getWorlds().stream().map(World::getName).toList());

        return List.of();
    }
}
