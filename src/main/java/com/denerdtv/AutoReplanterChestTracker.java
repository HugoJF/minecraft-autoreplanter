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
    private final List<Location> signLocations = new ArrayList<>();
    private final List<Chest> chests = new ArrayList<>();

    @Override
    public boolean handlesEvent(SignChangeEvent e) {
        String line = e.getLine(0);

        if (line == null) {
            return false;
        }

        if (!line.equalsIgnoreCase("[AutoReplanter]")) {
            return false;
        }

        Location attached = getAttachedLocation(e.getBlock());

        if (attached == null) {
            return false;
        }

        return attached.getBlock().getType() == Material.CHEST;
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
            return false;
        }

        signLocations.add(loc);
        updateChests();

        return true;
    }

    private boolean removeChest(Location loc) {
        if (signLocations.remove(loc)) {
            return false;
        }

        updateChests();

        return true;
    }

    private void updateChests() {
        chests.clear();

        for (int i = 0; i < this.signLocations.size(); i++) {
            try {
                // Remove if it's not a WallSign
                Location signLocation = this.signLocations.get(i);
                if (!(signLocation.getBlock().getBlockData() instanceof WallSign)) {
                    throw new Exception();
                }

                // Remove if is attached to nothing
                Location attached = getAttachedLocation(signLocation.getBlock());
                if (attached == null) {
                    throw new Exception();
                }

                // Remove if not attached to chest
                Chest chest = getChest(attached.getBlock());
                if (chest == null) {
                    throw new Exception();
                }

                chests.add(chest);
            } catch (Exception e) {
                Bukkit.getConsoleSender().sendMessage("Removed chest location while iterating over chests!");
                e.printStackTrace();
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
        this.signLocations.addAll(signLocations);
        updateChests();
    }
}
