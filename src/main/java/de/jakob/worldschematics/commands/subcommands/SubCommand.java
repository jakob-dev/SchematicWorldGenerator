package de.jakob.worldschematics.commands.subcommands;

import de.jakob.worldschematics.WorldSchematics;
import org.bukkit.command.CommandSender;

import java.util.List;

public abstract class SubCommand {

    public abstract void onCommand(WorldSchematics plugin, CommandSender sender, String label, String[] args);

    public abstract List<String> onTabComplete(WorldSchematics plugin, CommandSender sender, String label, String[] args);

}
