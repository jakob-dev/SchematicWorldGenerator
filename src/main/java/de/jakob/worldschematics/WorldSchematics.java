package de.jakob.worldschematics;

import de.jakob.worldschematics.commands.WorldSchematicCommand;
import de.jakob.worldschematics.listeners.InteractListener;
import de.jakob.worldschematics.schematics.SchematicManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class WorldSchematics extends JavaPlugin {

    private SchematicManager schematicManager;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        schematicManager = new SchematicManager(this);

        PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(new InteractListener(this), this);

        getCommand("worldschematic").setExecutor(new WorldSchematicCommand(this));

    }

    @Override
    public void onDisable() {
    }

    public String getPREFIX() {
        return ChatColor.GRAY + "[" + ChatColor.AQUA + "WS" + ChatColor.GRAY + "] » ";
    }

    public SchematicManager getSchematicManager() {
        return schematicManager;
    }
}
