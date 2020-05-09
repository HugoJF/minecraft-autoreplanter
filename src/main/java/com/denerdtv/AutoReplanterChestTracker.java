package com.denerdtv;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.SignChangeEvent;

import java.util.ArrayList;
import java.util.List;

import static com.denerdtv.AutoReplanter.SERVER_PREFIX;

public class AutoReplanterChestTracker implements CommandSignDefinition {
    private List<Location> signLocations = new ArrayList<>();
    private List<Chest> chests = new ArrayList<>();

    @Override
    public boolean handlesEvent(SignChangeEvent e) {
        if (e.getLine(0).equalsIgnoreCase("[AutoReplanter]")) {

            Location attached = getAttachedLocation(e.getBlock());

            return attached.getBlock().getType() == Material.CHEST;
        }

        return false;
    }

    @Override
    public void onSignCreate(SignChangeEvent e) {
        if (addChest(e.getBlock().getLocation())) {
            e.getPlayer().sendMessage(SERVER_PREFIX + "Chest added as seed source!");
        }
    }

    @Override
    public void onSignBreak(BlockBreakEvent e) {
        if (removeChest(e.getBlock().getLocation())) {
            e.getPlayer().sendMessage(SERVER_PREFIX + "Chest removed from AutoReplanter!");
        }
    }

    @Override
    public void onSignPhysics(BlockPhysicsEvent e) {
        if (removeChest(e.getBlock().getLocation())) {
            Bukkit.broadcastMessage((SERVER_PREFIX + "Chest removed from AutoReplanter by physics!"));
        }
    }

    private boolean addChest(Location loc) {
        if (!signLocations.contains(loc)) {
            signLocations.add(loc);
            updateChests();

            return true;
        }

        return false;
    }

    private boolean removeChest(Location loc) {
        if (signLocations.remove(loc)) {
            updateChests();

            return true;
        }

        return false;
    }

    private void updateChests() {
        chests.clear();

        for (int i = 0; i < this.signLocations.size(); i++) {
            Location signLocation = this.signLocations.get(i);
            if (signLocation.getBlock().getBlockData() instanceof WallSign) {
                Location attached = getAttachedLocation(signLocation.getBlock());
                Chest chest = getChest(attached.getBlock());

                if (chest == null) {
                    continue;
                }

                chests.add(chest);
            } else {
                Bukkit.getConsoleSender().sendMessage("Removed chest location while iterating over chests!");
                signLocations.remove(i--);
            }
        }
    }

    private Location getAttachedLocation(Block b) {
        BlockData blockData = b.getState().getBlockData();

        if (!(blockData instanceof Directional)) {
            return null;
        }

        Directional directional = (Directional) blockData;

        Block attached = b.getRelative(directional.getFacing().getOppositeFace());

        return attached.getLocation();
    }

    private Chest getChest(Block block) {
        if (block.getType() == Material.CHEST) {
            return (Chest) block.getState();
        } else {
            return null;
        }
    }

    public List<Chest> getChests() {
        return chests;
    }

    public List<Location> getSignLocations() {
        return signLocations;
    }

    public void setSignLocations(List<Location> signLocations) {
        this.signLocations = signLocations;
        updateChests();
    }
}
