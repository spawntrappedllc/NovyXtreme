package novyXtreme.utils;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.google.gson.reflect.TypeToken;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import novyXtreme.NovyXtreme;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import novyXtreme.Stargate;
import org.bukkit.plugin.Plugin;
import novyXtreme.utils.stargateUtils;

public class dbFunctions {


    private static ArrayList<Stargate> stargates = new ArrayList<Stargate>();
    public static ArrayList<Stargate> activeStargates = new ArrayList<>();
    static int stargateCost = NovyXtreme.getPlugin(NovyXtreme.class).getConfig().getInt("StargateCost");

    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    public static ArrayList<Stargate> getAllStargates(){
        return stargates;
    }
    public static Stargate getActivatedGate(String playername) {
        for (Stargate gate : activeStargates) {
            if (playername.equals(gate.getActivatedby())) {
                return gate;
            }
        }
        return null;
    }

    public static String isGateHere(Player player, BlockFace orientation, Location leverBlock) {
        //This needs cleaning up; remove nested IF statements
        for (Stargate gate : stargates) {
            // Checks if clicked lever matches any gate in database, else return null
            if (leverBlock.equals(gate.getLeverBlock())) {
                // Checks if gate orientation matches lever orientation
                if (gate.getGateOrientation().toString() == orientation.toString()) {
                    //TODO Return gate.. name?
                    return gate.getName();
                }
            }
        }
        return null;
    }

    public static Stargate getGatebyName(String gateName) {
        Stargate foundGate = null;
        ArrayList<Stargate> gateList = stargates;
        for (Stargate stargate : gateList) {
            if (stargate.getName().equals(gateName)) {
                foundGate = stargate;
                return foundGate;
            }
        }
        //TODO catch error no gate found;
        return foundGate;
    }

    private static String storedOwnerToRuntimeName(String stored) {
        if (stored == null) return null;
        try {
            UUID id = UUID.fromString(stored);
            OfflinePlayer op = Bukkit.getOfflinePlayer(id);
            String name = op.getName();
            if (name != null && !name.isEmpty()) {
                return name;
            } else {
                return stored;
            }
        } catch (IllegalArgumentException e) {
            return stored;
        }
    }

    private static String runtimeNameToStoredOwner(String ownerName) {
        if (ownerName == null) return null;
        try {
            UUID.fromString(ownerName);
            return ownerName;
        } catch (IllegalArgumentException ignored) {}

        try {
            OfflinePlayer op = Bukkit.getOfflinePlayer(ownerName);
            if (op != null) {
                UUID id = op.getUniqueId();
                if (id != null) return id.toString();
            }
        } catch (Throwable ignored) {}
        return ownerName;
    }

    public static void loadStargates() throws IOException {
        File file = new File(NovyXtreme.getPlugin().getDataFolder().getAbsolutePath() + "/stargatesList.json");
        if (!file.exists()) {
            Bukkit.broadcastMessage("File does not exist");
            return;
        }
        if (stargates == null) {
            Bukkit.broadcastMessage("Stargates is null");
            return;
        }

        Reader reader = new FileReader(file);
        List<StargateData> loadedData;
        try {
            loadedData = GSON.fromJson(reader, new TypeToken<ArrayList<StargateData>>(){}.getType());
            if (loadedData == null) {
                stargates = new ArrayList<>();
            } else {
                stargates = new ArrayList<>();
                for (StargateData d : loadedData) {
                    try {
                        Stargate gate = new Stargate();
                        //name
                        gate.setName(d.name);
                        // owner, stored as uuid but for some reason all the code uses username so do this hack
                        gate.setOwner(storedOwnerToRuntimeName(d.owner));

                        if (d.leverBlock != null) {
                            Location lever = d.leverBlock.toLocation();
                            gate.setLeverBlock(lever);
                        }

                        // facing
                        if (d.facing != null) {
                            try {
                                gate.setFacing(BlockFace.valueOf(d.facing));
                            } catch (IllegalArgumentException ignored) {
                                // skip if invalid
                            }
                        }

                        // timesVisited
                        gate.setTimesVisited(d.timesVisited);

                        // locked
                        gate.setLocked(d.locked);

                        // activatedby
                        gate.setActivatedby(d.activatedby);

                        if (gate.getLeverBlock() != null && gate.getFacing() != null) {
                            try {
                                gate.setTpCoordinates(stargateUtils.calcTeleportBlock(gate.getLeverBlock(), gate.getFacing()));
                            } catch (Throwable ignored) {}
                            try {
                                gate.setSignBlockLocation(stargateUtils.calcGateSignLocation(gate.getLeverBlock(), gate.getFacing()));
                            } catch (Throwable ignored) {}
                            try {
                                gate.setIrisBlocks(stargateUtils.calcIrisBlocks(gate.getLeverBlock(), gate.getFacing()));
                            } catch (Throwable ignored) {}
                            try {
                                gate.setPortalBlocks(stargateUtils.calcPortalBlocks(gate.getLeverBlock(), gate.getFacing()));
                            } catch (Throwable ignored) {}
                        }

                        dbFunctions.addGateToList(gate);

                        try { gate.updateGateSign(); } catch (Throwable ignored) {}
                    } catch (Throwable ignored) {
                    }
                }
            }
        } finally {
            reader.close();
        }
    }

    // Saves stargate list to File
    public static void saveStargates() throws IOException {
        File dataFolder = NovyXtreme.getPlugin().getDataFolder();
        File file = new File(dataFolder.getAbsolutePath() + "/stargatesList.json");
        file.getParentFile().mkdirs();

        // backup
        Path filePath = file.toPath();
        Path backupPath = null;
        try {
            if (Files.exists(filePath)) {
                File backupsDir = new File(dataFolder, "backups");
                backupsDir.mkdirs();

                String ts = Instant.now().toString().replace(":", "-");
                backupPath = Path.of(backupsDir.getAbsolutePath() + "/stargatesList.json.bak." + ts);
                Files.copy(filePath, backupPath, StandardCopyOption.REPLACE_EXISTING);
                Bukkit.getLogger().info("stargatesList.json: backup created -> " + backupPath.toString());
            }
        } catch (Throwable t) {
            Bukkit.getLogger().log(Level.WARNING, "Failed to create stargatesList.json backup before saving", t);
        }

        for (int i = 0; i < activeStargates.size(); i++) {
            try {
                activeStargates.get(i).setActive(false);
            } catch (Throwable ignored) {}
        }

        ArrayList<StargateData> out = new ArrayList<>();
        for (Stargate gate : stargates) {
            try {
                StargateData d = new StargateData();
                d.name = gate.getName();

                d.owner = runtimeNameToStoredOwner(gate.getOwner());

                d.facing = (gate.getFacing() == null) ? null : gate.getFacing().name();
                d.timesVisited = gate.getTimesVisited();
                d.locked = gate.isLocked();
                d.activatedby = gate.getActivatedby();

                Location lever = gate.getLeverBlock();
                if (lever == null) {
                    try {
                        lever = gate.getSignBlockLocation();
                    } catch (Throwable ignored) { lever = null; }
                }
                if (lever != null) {
                    d.leverBlock = LocationData.from(lever);
                } else {
                    d.leverBlock = null;
                }

                out.add(d);
            } catch (Throwable ignored) {
                Bukkit.getLogger().log(Level.WARNING, "Failed to serialize a stargate, skipping it: " + (gate != null ? gate.getName() : "unknown"), ignored);
            }
        }

        Path tempPath = Path.of(file.getAbsolutePath() + ".tmp");
        try {
            try (Writer writer = new FileWriter(tempPath.toFile(), false)) {
                GSON.toJson(out, new TypeToken<ArrayList<StargateData>>(){}.getType(), writer);
                writer.flush();
            }
            Files.move(tempPath, filePath, StandardCopyOption.REPLACE_EXISTING);
            Bukkit.getLogger().info("stargatesList.json saved successfully.");
        } catch (Throwable ex) {
            Bukkit.getLogger().log(Level.SEVERE, "CRITICAL: Failed to save stargatesList.json", ex);

            // attempt to restore backup
            try {
                if (backupPath != null && Files.exists(backupPath)) {
                    Files.copy(backupPath, filePath, StandardCopyOption.REPLACE_EXISTING);
                    Bukkit.getLogger().severe("stargatesList.json restored from backup: " + backupPath.toString());
                }
            } catch (Throwable restoreEx) {
                Bukkit.getLogger().log(Level.SEVERE, "Failed to restore stargatesList.json from backup", restoreEx);
            }

            try {
                Plugin plugin = NovyXtreme.getPlugin();
                if (plugin != null && Bukkit.getPluginManager().isPluginEnabled(plugin)) {
                    Bukkit.getLogger().severe("DISABLING PLUGIN: " + plugin.getName() + " due to failure saving stargatesList.json");
                    Bukkit.getPluginManager().disablePlugin(plugin);
                }
            } catch (Throwable disableEx) {
                Bukkit.getLogger().log(Level.SEVERE, "Failed while attempting to disable the plugin after save failure", disableEx);
            }

            throw new IOException("Failed to save stargatesList.json", ex);
        } finally {
            try {
                if (Files.exists(tempPath)) Files.deleteIfExists(tempPath);
            } catch (Throwable ignored) {}
        }
    }



    public static void addGateToList(Stargate stargate) {
        stargates.add(stargate);
    }

    //TODO These should be combined into one function with optional parameters
    public static String getStargateListToStringVerbose(String ownerName) {
        String stargateListString = null;

        if (ownerName == null) {
            stargateListString = "Stargates ( Name | Owner | Times Visited | TpCoords )" ;
            for (Stargate stargate : stargates) {
                stargateListString = stargateListString + "\n"+ChatColor.GRAY + stargate.getName() + ChatColor.DARK_GRAY + " | " + ChatColor.GRAY + stargate.getOwner() + ChatColor.DARK_GRAY + " | " + ChatColor.GRAY + stargate.getTimesVisited() + ChatColor.DARK_GRAY + " | " + ChatColor.GRAY + "( " + stargate.getTpCoordinates().getBlockX() + "," + stargate.getTpCoordinates().getBlockY() + "," + stargate.getTpCoordinates().getBlockZ() + " )";
            }
        } else {
            stargateListString = "Stargates Owned by " + ownerName + ":";
            for (Stargate stargate : stargates) {
                if (stargate.getOwner().equals(ownerName)) {
                    stargateListString = stargateListString + "\n"+ChatColor.GRAY + "Name: " + stargate.getName();
                }
            }
        }
        return stargateListString;
    }

    public static String getStargateListToString() {
        String stargateListString = "Stargates ( Name | Owner )";
        for (Stargate stargate : stargates) {
            stargateListString = stargateListString + "\n" + ChatColor.GRAY + stargate.getName() + ChatColor.DARK_GRAY + " | " + ChatColor.GRAY + stargate.getOwner();
        }
        return stargateListString;
    }

    public static String getStargateListFromOwnerVerbose(String ownerName) {
        String stargateListString = "Stargates Owned by " + ownerName + " ( Name | Times Visited | TpCoords )";
        for (Stargate stargate : stargates) {
            if (stargate.getOwner().equals(ownerName)) {
                stargateListString = stargateListString + "\n" + ChatColor.GRAY + stargate.getName() + ChatColor.DARK_GRAY + " | " + ChatColor.GRAY + stargate.getTimesVisited() + ChatColor.DARK_GRAY + " | " + ChatColor.GRAY + "( " + stargate.getTpCoordinates().getBlockX() + "," + stargate.getTpCoordinates().getBlockY() + "," + stargate.getTpCoordinates().getBlockZ() + " )";
            }
        }
        return stargateListString;
    }

    public static String getStargateListFromOwner(String ownerName) {
        String stargateListString = "Stargates Owned by " + ownerName + ":";
        for (Stargate stargate : stargates) {
            if (stargate.getOwner().equals(ownerName)) {
                stargateListString = stargateListString + "\n" +ChatColor.GRAY + ChatColor.GRAY + stargate.getName();
            }
        }
        return stargateListString;
    }

    public static int getStargateCountByOwner(String ownerName) {
        int count = 0;
        if (ownerName == null) return 0;
        for (Stargate s : stargates) {
            if (s.getOwner() != null && s.getOwner().equalsIgnoreCase(ownerName)) {
                count++;
            }
        }
        return count;
    }

    public static boolean isOwnedBy(String playerName, String gateName) {
        if (playerName == null || gateName == null) return false;

        Stargate gate = getGatebyName(gateName);
        if (gate == null) return false;

        String owner = gate.getOwner();
        return owner != null && owner.equalsIgnoreCase(playerName);
    }

    /**
     * Removes all stargates owned by ownerName except the gate with name keepGateName.
     */
    public static void removeAllGatesForOwnerExcept(String ownerName, String keepGateName) {
        if (ownerName == null) return;

        removeAllGatesForOwnerExcept(ownerName, keepGateName, true);
    }

    public static void removeAllGatesForOwnerExcept(String ownerName, String keepGateName, boolean doNotify) {
        if (ownerName == null) return;

        Iterator<Stargate> it = stargates.iterator();
        while (it.hasNext()) {
            Stargate s = it.next();
            if (s.getOwner() != null && s.getOwner().equalsIgnoreCase(ownerName) && !s.getName().equals(keepGateName)) {
                // Deactivate active gates
                try {
                    if (s.isActive()) {
                        s.setActive(false);
                    }
                } catch (Exception ignored) {}

                // Break gate sign
                try {
                    if (s.getSignBlockLocation() != null) {
                        s.getSignBlockLocation().getBlock().setType(Material.AIR);
                    }
                } catch (Exception ignored) {}

                // inform player
                if (doNotify) {
                    Player owner = Bukkit.getPlayer(s.getOwner());
                    if (owner != null) {
                        messageUtils.sendMessage(
                                "One of your extra stargates (" + s.getName() + ") was removed because you no longer have premium.",
                                owner
                        );
                    }
                }
                it.remove();
            }
        }
    }

    public static boolean removeGateByName(String gatename) {
        for (Stargate stargate : stargates) {
            if (stargate.getName().equals(gatename)) {
                if (stargate.isActive()) {
                    stargate.setActive(false);
                }

                // Break sign when stargate is destroyed/removed
                stargate.getSignBlockLocation().getBlock().setType(Material.AIR);

                Player owner = Bukkit.getPlayer(stargate.getOwner());
                if (owner != null) {
                    messageUtils.sendMessage(
                            "Gate: " + stargate.getName() + " destroyed",
                            owner
                    );
                }

                stargates.remove(stargate);
                return true;
            }
        }
        return false;
    }

    // Disable all gates, used by /nxforce
    public static boolean deactivateAllGates() {
        Stargate activegate;
        int gates = activeStargates.size();
        for (int i = 0; i < gates; i++) {
            if (activeStargates.size() > 0) {
                activegate = activeStargates.get(0);
                activegate.setActive(false);
            }
        }
        return true;
    }

    private static class LocationData {
        public String world;
        public double x, y, z;
        public float yaw, pitch;

        public static LocationData from(Location loc) {
            if (loc == null) return null;
            LocationData d = new LocationData();
            World w = loc.getWorld();
            d.world = (w == null) ? null : w.getName();
            d.x = loc.getX();
            d.y = loc.getY();
            d.z = loc.getZ();
            d.yaw = loc.getYaw();
            d.pitch = loc.getPitch();
            return d;
        }

        public Location toLocation() {
            if (world == null) return null;
            World w = Bukkit.getWorld(world);
            if (w == null) return null;
            return new Location(w, x, y, z, yaw, pitch);
        }
    }

    private static class StargateData {
        public String name;
        public String owner;
        public LocationData leverBlock;
        public String facing;
        public int timesVisited = 0;
        public boolean locked = false;
        public String activatedby;
    }
}
