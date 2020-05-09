package com.denerdtv;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.bukkit.ChatColor.*;

public class AutoReplanter extends JavaPlugin implements CommandExecutor, Listener {

    // Constants
    public static final String NOTIFICATION = "notification";
    public static final String REPLANT = "replant";
    public static final String CHEST = "chest";

    private final Vector UP = new Vector(0, 1, 0);
    private final Vector DOWN = new Vector(0, -1, 0);

    private final int MAX_REPLANTS_PER_SECOND = 1;

    // Static variables
    private static AutoReplanter instance = null;

    public static final String COMMAND = "autoreplant";
    public static final String SERVER_PREFIX = BLACK + "[" + RED + "SERVER" + BLACK + "] " + RESET;

    // Stored references
    private PluginManager plugin;

    private final CommandSignManager csm = new CommandSignManager();
    private final AutoReplanterChestTracker arcs = new AutoReplanterChestTracker();

    // Persistence
    private File file;
    private String configPath;
    private YamlConfiguration config;

    // Helpers
    private final HashMap<Material, Material> cropSeeds = new HashMap<>();
    private final Set<Material> hoes = new HashSet<>();

    // Plugin runtime
    private boolean showing = false;
    private final HashMap<Player, Boolean> enabled = new HashMap<>();
    private final HashSet<Location> emptyChests = new HashSet<>();
    private final HashSet<Location> unplantedCrops = new HashSet<>();
    private final HashMap<Location, Material> cropPreferences = new HashMap<>();
    private final AutoReplanterCommand autoReplanterCommandListener = new AutoReplanterCommand();
    private ParticleSystem particleSystem;


    public AutoReplanter() throws Exception {
        if (instance != null) {
            throw new Exception("Multiple singletons");
        } else {
            AutoReplanter.instance = this;
        }

        this.csm.addDefinition(arcs);
    }

    public static AutoReplanter getInstance() {
        return AutoReplanter.instance;
    }

    /**
     * Auto-Hoeing
     * Auto-Bonemealing
     * YAML configurable
     */

    /**
     * Library: AutoCommands
     * Library: GUI Interfaces
     * Library: Auto-serialization
     */

    public void onEnable() {
        this.plugin = Bukkit.getPluginManager();
        init();

        preparePersistence();
        restoreState();

        registerCommands();
        registerListener();
        registerTimer();
    }

    private void init() {
        this.particleSystem = new ParticleSystem(Bukkit.getWorld("world"));

        particleSystem.addConfiguration(CHEST, ParticleConfiguration.create(Particle.VILLAGER_HAPPY).setAmount(15).setOffset(0.40));
        particleSystem.addConfiguration(REPLANT, ParticleConfiguration.create(Particle.VILLAGER_HAPPY).setAmount(15).setOffset(0.25));
        particleSystem.addConfiguration(NOTIFICATION, ParticleConfiguration.create(Particle.VILLAGER_ANGRY).setAmount(1).setOffset(0));

        cropSeeds.put(Material.POTATOES, Material.POTATO);
        cropSeeds.put(Material.WHEAT, Material.WHEAT_SEEDS);
        cropSeeds.put(Material.CARROTS, Material.CARROT);
        cropSeeds.put(Material.BEETROOTS, Material.BEETROOT_SEEDS);

        hoes.add(Material.WOODEN_HOE);
        hoes.add(Material.GOLDEN_HOE);
        hoes.add(Material.STONE_HOE);
        hoes.add(Material.IRON_HOE);
        hoes.add(Material.DIAMOND_HOE);
    }

    private void registerCommands() {
        getCommand("autoreplant").setExecutor(this.autoReplanterCommandListener);
        getCommand("autoreplant").setTabCompleter(this.autoReplanterCommandListener);
    }

    private void registerTimer() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, this::runTicks, 20L, 20L);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, this::showParticles, 20L, 20L);
    }

    private void registerListener() {
        this.plugin.registerEvents(this, this);
        this.plugin.registerEvents(csm, this);
        this.plugin.registerEvents(autoReplanterCommandListener, this);
    }

    private void saveState() {
        this.config.set("showing", showing);
        List<Map<String, Object>> cps = new LinkedList<>();

        for (Location l : this.cropPreferences.keySet()) {
            Map<String, Object> cropPreference = new HashMap<>();
            cropPreference.put("location", l);
            cropPreference.put("preference", this.cropPreferences.get(l).toString());
            cps.add(cropPreference);
        }
        this.config.set("farms", cps);
        this.config.set("chests-locations", this.arcs.getSignLocations());

        try {
            this.config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void restoreState() {
        List<Map<?, ?>> farms = this.config.getMapList("farms");

        for (Map<?, ?> x : farms) {
            Location l = (Location) x.get("location");
            Material m = Material.getMaterial((String) x.get("preference"));
            this.cropPreferences.put(l, m);
        }

        this.showing = this.config.getBoolean("showing");
        this.arcs.setSignLocations((List<Location>) this.config.getList("chests-locations", new ArrayList<Location>()));
    }


    private void preparePersistence() {
        this.configPath = this.getDataFolder() + "/farms-tracked.yml";
        this.file = new File(configPath);
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public void onDisable() {
        saveState();
    }

    private void showParticles() {
        // Chests that are empty
        for (Location l : this.emptyChests) {
            this.particleSystem.spawnCenter(NOTIFICATION, l);
        }

        // Crops that cannot be planted
        for (Location l : this.unplantedCrops) {
            this.particleSystem.spawnCenter(NOTIFICATION, l);
        }

        if (!showing) return;

        // Currently tracked crops
        for (Location loc : cropPreferences.keySet()) {
            this.particleSystem.spawnCenter(NOTIFICATION, loc.clone().add(UP));
        }

        // Currently tracked chests
        for (Chest c : this.arcs.getChests()) {
            this.particleSystem.spawnCenter(CHEST, c.getLocation());
        }
    }

    private void runTicks() {
        int checks = 0;
        int replantCounter = 0;

        while (true) {
            if (cropPreferences.size() == 0) return;

            for (Location l : cropPreferences.keySet()) {
                // Too many checks in this tick
                if (checks++ >= cropPreferences.size()) {
                    return;
                }

                // Too many replants in the this tick
                if (replantCounter >= MAX_REPLANTS_PER_SECOND) {
                    return;
                }

                // Checks block
                if (check(l)) {
                    replantCounter++;
                    break;
                }
            }
        }

    }

    private boolean reHoe(Location location) {
        Chest chest = getClosestChest(location);
        if (chest == null) {
            return false;
        }

        for (int i = 0; i < chest.getInventory().getSize(); i++) {
            // Check if item is a kind of hoe
            ItemStack item = chest.getInventory().getItem(i);
            if (item == null || !hoes.contains(item.getType())) {
                continue;
            }

            // Assure item is Damageable
            ItemMeta meta = item.getItemMeta();
            if (!(meta instanceof Damageable)) {
                continue;
            }

            // Do damage to hoe
            Damageable damage = (Damageable) meta;
            damage.setDamage(damage.getDamage() + (int) Math.round(Math.random() * 5));
            location.getBlock().setType(Material.FARMLAND);
            item.setItemMeta(meta);

            return true;
        }

        return false;
    }

    private boolean check(Location l) {
        Material mat = l.getBlock().getType();

        if (mat == Material.AIR) {
            cropPreferences.remove(l);
            unplantedCrops.remove(l);
        }

        if (mat == Material.DIRT) {
            return this.reHoe(l);
        }

        Block cropBlock = l.getWorld().getBlockAt(l.clone().add(UP));
        Material cropSeed = cropSeeds.get(cropPreferences.get(l));

        if (cropBlock.getType() != Material.AIR) {
            return false;
        }

        Chest mainChest = getClosestChest(l);

        if (mainChest == null) {
            return false;
        }

        if (!mainChest.getInventory().containsAtLeast(new ItemStack(cropSeed, 1), 1)) {
            emptyChests.add(mainChest.getLocation());
            unplantedCrops.add(cropBlock.getLocation());

            return false;
        }

        emptyChests.remove(mainChest);
        unplantedCrops.remove(cropBlock.getLocation());

        cropBlock.setType(cropPreferences.get(l));

        mainChest.getInventory().removeItem(new ItemStack(cropSeed, 1));

        particleSystem.spawnCenter(REPLANT, cropBlock.getLocation());

        return true;
    }

    private Chest getClosestChest(Location loc) {
        List<Chest> chests = arcs.getChests();
        if (chests == null) return null;

        if (chests.size() == 0) return null;
        Chest closestChest = chests.get(0);

        double closestDistance = closestChest.getLocation().distance(loc);

        for (Chest chest : this.arcs.getChests()) {
            double dist = chest.getLocation().distance(loc);
            if (dist < closestDistance) {
                closestDistance = dist;
                closestChest = chest;
            }
        }

        return closestChest;
    }

    @EventHandler
    public void onBlockInteract(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) return;
        if (e.getClickedBlock().getType() == Material.CHEST) {
            emptyChests.remove(e.getClickedBlock().getLocation());
        }

    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if (e.getBlock().getType() == Material.CHEST) {
            emptyChests.remove(e.getBlock().getLocation());
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        InventoryHolder ih = e.getInventory().getHolder();
        if (ih instanceof Chest) {
            emptyChests.remove(((Chest) ih).getLocation());
        }
    }


    @EventHandler
    public void OnBlockPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();

        // Check if player has AutoReplant enabled

        if (!enabled.getOrDefault(p, false)) return;

        Block b = e.getBlock();

        // Check if block placed is wheat
        for (Material crop : this.cropSeeds.keySet()) {
            if (b.getType() != crop) {
                continue;
            }

            Location loc = b.getLocation().clone().add(DOWN);

            if (loc.getBlock().getType() != Material.FARMLAND) {
                continue;
            }

            // Check if bottom block is a farm and we don't track it
            if (!cropPreferences.keySet().contains(loc)) {
                // Track it
                cropPreferences.put(loc, crop);
                this.particleSystem.spawnCenter(REPLANT, loc.clone().add(UP));
                return;
            }
        }
    }

    public boolean getShowing() {
        return showing;
    }

    public void setShowing(boolean showing) {
        this.showing = showing;
    }

    public boolean getEnabled(Player p) {
        return this.enabled.getOrDefault(p, false);
    }

    public void setEnabled(Player p, boolean enabled) {
        this.enabled.put(p, enabled);
    }

    public int getLocationCount() {
        return this.cropPreferences.size();
    }

    public List<Chest> getChests() {
        return this.arcs.getChests();
    }
}
