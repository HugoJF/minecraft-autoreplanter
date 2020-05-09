package com.denerdtv;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;

import java.util.ArrayList;
import java.util.List;

import static com.denerdtv.AutoReplanter.SERVER_PREFIX;

public class DEPRECATED implements Listener {
    private final List<Chest> chests = new ArrayList<>();

    @EventHandler
    public void onSignChangeEvent(SignChangeEvent e) {
        String line = e.getLine(0);

        if (line == null || !line.equalsIgnoreCase("[AutoReplanter]")) {
            return;
        }

        BlockData blockData = e.getBlock().getBlockData();

        if (!(blockData instanceof Directional)) {
            return;
        }

        Directional directional = (Directional) blockData;

        Block attached = e.getBlock().getRelative(directional.getFacing().getOppositeFace());

        if (attached.getType() != Material.CHEST) {
            return;
        }

        this.chests.add((Chest) attached.getState());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        BlockData blockData = b.getBlockData();

        if (!(blockData instanceof Sign)) {
            return;
        }

        if (!(blockData instanceof Directional)) {
            return;
        }

        Directional directional = (Directional) blockData;

        Block attached = b.getRelative(directional.getFacing().getOppositeFace());
        Chest c = (Chest) attached.getState();

        if (chests.remove(c)) {
            e.getPlayer().sendMessage(SERVER_PREFIX + "Chest removed from AutoReplanter!");
        }
    }
}
