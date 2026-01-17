package de.jakob.worldschematics.listeners;

import de.jakob.worldschematics.WorldSchematics;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class InteractListener implements Listener {

    private final WorldSchematics plugin;

    public InteractListener(WorldSchematics plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {

        Player player = event.getPlayer();

        if (!player.hasPermission("ws.select") || player.getGameMode() != GameMode.CREATIVE) {
            return;
        }
        if (player.getInventory().getItemInMainHand().getType() != Material.WOODEN_PICKAXE || event.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();

        if (clickedBlock == null) {
            return;
        }

        Location location = clickedBlock.getLocation();
        Chunk chunk = location.getChunk();

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            plugin.getSchematicManager().handleSelection(player.getUniqueId(), location, null);
            player.sendMessage(plugin.getPREFIX() + "Chunk-1 marked [§b" + chunk.getX() + "§7:§b" + chunk.getZ() + "§7]");
        }


        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            plugin.getSchematicManager().handleSelection(player.getUniqueId(), null, location);
            player.sendMessage(plugin.getPREFIX() + "Chunk-2 marked [§b" + chunk.getX() + "§7:§b" + chunk.getZ() + "§7]");
        }

        event.setCancelled(true);


    }

    @EventHandler
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        plugin.getSchematicManager().getSchematicSelections().remove(event.getPlayer().getUniqueId());
    }


}
