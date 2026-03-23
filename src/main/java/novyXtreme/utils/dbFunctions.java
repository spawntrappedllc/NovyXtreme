package novyXtreme.utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import novyXtreme.NovyXtreme;
import novyXtreme.Stargate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.sql.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;
import java.util.logging.Level;

public class dbFunctions {

    private static final ArrayList<Stargate> stargates = new ArrayList<>();
    public static final ArrayList<Stargate> activeStargates = new ArrayList<>();

    private static HikariDataSource dataSource;

    public static void initDatabase(Plugin plugin) throws SQLException {
        FileConfiguration config = plugin.getConfig();
        String host     = config.getString("Database.host",     "localhost");
        int    port     = config.getInt   ("Database.port",     3306);
        String dbName   = config.getString("Database.name",     "novyxtreme");
        String username = config.getString("Database.username", "root");
        String password = config.getString("Database.password", "");

        HikariConfig hikari = new HikariConfig();
        hikari.setDriverClassName("org.mariadb.jdbc.Driver");
        hikari.setJdbcUrl("jdbc:mariadb://" + host + ":" + port + "/" + dbName
                + "?useUnicode=true&characterEncoding=utf8");
        hikari.setUsername(username);
        hikari.setPassword(password);
        hikari.setMaximumPoolSize(5);
        hikari.setMinimumIdle(1);
        hikari.setConnectionTimeout(30_000);
        hikari.setIdleTimeout(600_000);
        hikari.setMaxLifetime(1_800_000);
        hikari.setPoolName("NovyXtreme-DB");

        dataSource = new HikariDataSource(hikari);
        createTables();
        Bukkit.getLogger().info("[NovyXtreme] Database connection established.");
    }

    public static void closeDatabase() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    private static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("Database not initialised.");
        }
        return dataSource.getConnection();
    }

    // schema

    private static void createTables() throws SQLException {
        String sql =
                "CREATE TABLE IF NOT EXISTS stargates (" +
                        "  name         VARCHAR(64)  NOT NULL PRIMARY KEY," +
                        "  owner        CHAR(36)     NOT NULL," +
                        "  world        VARCHAR(64)  NOT NULL," +
                        "  lever_x      DOUBLE       NOT NULL," +
                        "  lever_y      DOUBLE       NOT NULL," +
                        "  lever_z      DOUBLE       NOT NULL," +
                        "  lever_yaw    FLOAT        NOT NULL DEFAULT 0," +
                        "  lever_pitch  FLOAT        NOT NULL DEFAULT 0," +
                        "  facing       VARCHAR(16)," +
                        "  times_visited INT         NOT NULL DEFAULT 0," +
                        "  locked       TINYINT(1)   NOT NULL DEFAULT 0," +
                        "  activated_by VARCHAR(64)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

        try (Connection conn = getConnection();
             Statement  stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    // load and save

    public static void loadStargates() throws SQLException {
        stargates.clear();

        String sql = "SELECT name, owner, world, lever_x, lever_y, lever_z, " +
                "lever_yaw, lever_pitch, facing, times_visited, locked, activated_by " +
                "FROM stargates";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                try {
                    Stargate gate = new Stargate();
                    gate.setName(rs.getString("name"));

                    try {
                        gate.setOwnerUuid(UUID.fromString(rs.getString("owner")));
                    } catch (IllegalArgumentException e) {
                        Bukkit.getLogger().warning("[NovyXtreme] Skipping gate '" + rs.getString("name")
                                + "': invalid owner UUID '" + rs.getString("owner") + "'.");
                        continue;
                    }

                    String worldName = rs.getString("world");
                    World world = Bukkit.getWorld(worldName);
                    if (world == null) {
                        Bukkit.getLogger().warning("[NovyXtreme] Skipping gate '" + gate.getName()
                                + "': world '" + worldName + "' is not loaded.");
                        continue;
                    }

                    Location lever = new Location(world,
                            rs.getDouble("lever_x"),
                            rs.getDouble("lever_y"),
                            rs.getDouble("lever_z"),
                            rs.getFloat("lever_yaw"),
                            rs.getFloat("lever_pitch"));
                    gate.setLeverBlock(lever);

                    String facingStr = rs.getString("facing");
                    if (facingStr != null) {
                        try { gate.setFacing(BlockFace.valueOf(facingStr)); }
                        catch (IllegalArgumentException ignored) {}
                    }

                    gate.setTimesVisited(rs.getInt("times_visited"));
                    gate.setLockedSilent(rs.getBoolean("locked"));
                    gate.setActivatedby(rs.getString("activated_by"));

                    //get from lever pos
                    if (gate.getLeverBlock() != null && gate.getFacing() != null) {
                        try { gate.setTpCoordinates(stargateUtils.calcTeleportBlock(gate.getLeverBlock(), gate.getFacing())); }  catch (Throwable ignored) {}
                        try { gate.setSignBlockLocation(stargateUtils.calcGateSignLocation(gate.getLeverBlock(), gate.getFacing())); } catch (Throwable ignored) {}
                        try { gate.setIrisBlocks(stargateUtils.calcIrisBlocks(gate.getLeverBlock(), gate.getFacing())); }           catch (Throwable ignored) {}
                        try { gate.setPortalBlocks(stargateUtils.calcPortalBlocks(gate.getLeverBlock(), gate.getFacing())); }       catch (Throwable ignored) {}
                    }

                    stargates.add(gate);
                    try { gate.updateGateSign(); } catch (Throwable ignored) {}

                } catch (Throwable t) {
                    Bukkit.getLogger().log(Level.WARNING, "[NovyXtreme] Failed to load a stargate row from DB.", t);
                }
            }
        }

        Bukkit.getLogger().info("[NovyXtreme] Loaded " + stargates.size() + " stargates from database.");
    }

    public static void saveStargates() throws SQLException {
        int count = activeStargates.size();
        for (int i = 0; i < count; i++) {
            if (!activeStargates.isEmpty()) {
                try { activeStargates.get(0).setActive(false); } catch (Throwable ignored) {}
            }
        }

        String upsert =
                "INSERT INTO stargates " +
                        "  (name, owner, world, lever_x, lever_y, lever_z, lever_yaw, lever_pitch, " +
                        "   facing, times_visited, locked, activated_by) " +
                        "VALUES (?,?,?,?,?,?,?,?,?,?,?,?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "  owner=VALUES(owner), world=VALUES(world), " +
                        "  lever_x=VALUES(lever_x), lever_y=VALUES(lever_y), lever_z=VALUES(lever_z), " +
                        "  lever_yaw=VALUES(lever_yaw), lever_pitch=VALUES(lever_pitch), " +
                        "  facing=VALUES(facing), times_visited=VALUES(times_visited), " +
                        "  locked=VALUES(locked), activated_by=VALUES(activated_by)";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(upsert)) {

            for (Stargate gate : stargates) {
                try {
                    Location lever = gate.getLeverBlock();
                    if (lever == null || lever.getWorld() == null) continue;

                    ps.setString (1,  gate.getName());
                    ps.setString (2,  gate.getOwnerUuid().toString());
                    ps.setString (3,  lever.getWorld().getName());
                    ps.setDouble (4,  lever.getX());
                    ps.setDouble (5,  lever.getY());
                    ps.setDouble (6,  lever.getZ());
                    ps.setFloat  (7,  lever.getYaw());
                    ps.setFloat  (8,  lever.getPitch());
                    ps.setString (9,  gate.getFacing() == null ? null : gate.getFacing().name());
                    ps.setInt    (10, gate.getTimesVisited());
                    ps.setBoolean(11, gate.isLocked());
                    ps.setString (12, gate.getActivatedby());
                    ps.addBatch();
                } catch (Throwable t) {
                    Bukkit.getLogger().log(Level.WARNING,
                            "[NovyXtreme] Failed to serialise gate '"
                                    + (gate != null ? gate.getName() : "unknown") + "' — skipping.", t);
                }
            }
            ps.executeBatch();
        }

        Bukkit.getLogger().info("[NovyXtreme] Stargates saved to database successfully.");
    }

    private static void upsertGate(Stargate gate) {
        if (dataSource == null || gate == null) return;

        String upsert =
                "INSERT INTO stargates " +
                        "  (name, owner, world, lever_x, lever_y, lever_z, lever_yaw, lever_pitch, " +
                        "   facing, times_visited, locked, activated_by) " +
                        "VALUES (?,?,?,?,?,?,?,?,?,?,?,?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "  owner=VALUES(owner), world=VALUES(world), " +
                        "  lever_x=VALUES(lever_x), lever_y=VALUES(lever_y), lever_z=VALUES(lever_z), " +
                        "  lever_yaw=VALUES(lever_yaw), lever_pitch=VALUES(lever_pitch), " +
                        "  facing=VALUES(facing), times_visited=VALUES(times_visited), " +
                        "  locked=VALUES(locked), activated_by=VALUES(activated_by)";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(upsert)) {

            Location lever = gate.getLeverBlock();
            if (lever == null || lever.getWorld() == null) return;

            ps.setString (1,  gate.getName());
            ps.setString (2,  gate.getOwnerUuid().toString());
            ps.setString (3,  lever.getWorld().getName());
            ps.setDouble (4,  lever.getX());
            ps.setDouble (5,  lever.getY());
            ps.setDouble (6,  lever.getZ());
            ps.setFloat  (7,  lever.getYaw());
            ps.setFloat  (8,  lever.getPitch());
            ps.setString (9,  gate.getFacing() == null ? null : gate.getFacing().name());
            ps.setInt    (10, gate.getTimesVisited());
            ps.setBoolean(11, gate.isLocked());
            ps.setString (12, gate.getActivatedby());
            ps.executeUpdate();

        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.WARNING,
                    "[NovyXtreme] Failed to upsert gate '" + gate.getName() + "' in DB.", e);
        }
    }

    public static void updateGateInDb(Stargate gate) {
        upsertGate(gate);
    }

    private static void deleteGateFromDb(String gateName) {
        if (dataSource == null) return;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM stargates WHERE name = ?")) {
            ps.setString(1, gateName);
            ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.WARNING,
                    "[NovyXtreme] Failed to delete gate '" + gateName + "' from DB.", e);
        }
    }

    public static ArrayList<Stargate> getAllStargates() {
        return stargates;
    }

    public static Stargate getActivatedGate(String playerName) {
        for (Stargate gate : activeStargates) {
            if (playerName.equals(gate.getActivatedby())) return gate;
        }
        return null;
    }

    public static String isGateHere(Player player, BlockFace orientation, Location leverBlock) {
        for (Stargate gate : stargates) {
            if (leverBlock.equals(gate.getLeverBlock())
                    && gate.getGateOrientation().equals(orientation)) {
                return gate.getName();
            }
        }
        return null;
    }

    public static Stargate getGatebyName(String gateName) {
        for (Stargate gate : stargates) {
            if (gate.getName().equals(gateName)) return gate;
        }
        return null;
    }

    public static void addGateToList(Stargate gate) {
        stargates.add(gate);
        upsertGate(gate);
    }

    public static boolean removeGateByName(String gateName) {
        Iterator<Stargate> it = stargates.iterator();
        while (it.hasNext()) {
            Stargate gate = it.next();
            if (gate.getName().equals(gateName)) {
                if (gate.isActive()) gate.setActive(false);
                gate.getSignBlockLocation().getBlock().setType(Material.AIR);

                Player owner = Bukkit.getPlayer(gate.getOwnerUuid());
                if (owner != null) {
                    messageUtils.sendMessage("Gate: " + gate.getName() + " destroyed", owner);
                }

                it.remove();
                deleteGateFromDb(gateName);
                return true;
            }
        }
        return false;
    }

    public static void removeAllGatesForOwnerExcept(UUID ownerUuid, String keepGateName) {
        removeAllGatesForOwnerExcept(ownerUuid, keepGateName, true);
    }

    public static void removeAllGatesForOwnerExcept(UUID ownerUuid, String keepGateName, boolean doNotify) {
        if (ownerUuid == null) return;

        Iterator<Stargate> it = stargates.iterator();
        while (it.hasNext()) {
            Stargate gate = it.next();
            if (ownerUuid.equals(gate.getOwnerUuid())
                    && !gate.getName().equals(keepGateName)) {

                try { if (gate.isActive()) gate.setActive(false); } catch (Exception ignored) {}

                try {
                    if (gate.getSignBlockLocation() != null) {
                        gate.getSignBlockLocation().getBlock().setType(Material.AIR);
                    }
                } catch (Exception ignored) {}

                if (doNotify) {
                    Player owner = Bukkit.getPlayer(ownerUuid);
                    if (owner != null) {
                        messageUtils.sendMessage(
                                "One of your extra stargates (" + gate.getName()
                                        + ") was removed because you no longer have premium.", owner);
                    }
                }

                deleteGateFromDb(gate.getName());
                it.remove();
            }
        }
    }

    public static boolean deactivateAllGates() {
        int gates = activeStargates.size();
        for (int i = 0; i < gates; i++) {
            if (!activeStargates.isEmpty()) {
                activeStargates.get(0).setActive(false);
            }
        }
        return true;
    }

    public static int getStargateCountByOwner(UUID ownerUuid) {
        if (ownerUuid == null) return 0;
        int count = 0;
        for (Stargate gate : stargates) {
            if (ownerUuid.equals(gate.getOwnerUuid())) count++;
        }
        return count;
    }

    public static boolean isOwnedBy(UUID ownerUuid, String gateName) {
        if (ownerUuid == null || gateName == null) return false;
        Stargate gate = getGatebyName(gateName);
        return gate != null && ownerUuid.equals(gate.getOwnerUuid());
    }

    public static String getStargateListToStringVerbose(UUID ownerUuid) {
        StringBuilder out = new StringBuilder();
        if (ownerUuid == null) {
            out.append("Stargates ( Name | Owner | Times Visited | TpCoords )");
            for (Stargate gate : stargates) {
                out.append("\n").append(ChatColor.GRAY).append(gate.getName())
                        .append(ChatColor.DARK_GRAY).append(" | ").append(ChatColor.GRAY).append(gate.getOwnerName())
                        .append(ChatColor.DARK_GRAY).append(" | ").append(ChatColor.GRAY).append(gate.getTimesVisited())
                        .append(ChatColor.DARK_GRAY).append(" | ").append(ChatColor.GRAY)
                        .append("( ").append(gate.getTpCoordinates().getBlockX())
                        .append(",").append(gate.getTpCoordinates().getBlockY())
                        .append(",").append(gate.getTpCoordinates().getBlockZ()).append(" )");
            }
        } else {
            out.append("Stargates Owned by ").append(gate_ownerName(ownerUuid)).append(":");
            for (Stargate gate : stargates) {
                if (ownerUuid.equals(gate.getOwnerUuid())) {
                    out.append("\n").append(ChatColor.GRAY).append("Name: ").append(gate.getName());
                }
            }
        }
        return out.toString();
    }

    public static String getStargateListToString() {
        StringBuilder out = new StringBuilder("Stargates ( Name | Owner )");
        for (Stargate gate : stargates) {
            out.append("\n").append(ChatColor.GRAY).append(gate.getName())
                    .append(ChatColor.DARK_GRAY).append(" | ").append(ChatColor.GRAY).append(gate.getOwnerName());
        }
        return out.toString();
    }

    public static String getStargateListFromOwnerVerbose(UUID ownerUuid) {
        StringBuilder out = new StringBuilder(
                "Stargates Owned by " + gate_ownerName(ownerUuid) + " ( Name | Times Visited | TpCoords )");
        for (Stargate gate : stargates) {
            if (ownerUuid.equals(gate.getOwnerUuid())) {
                out.append("\n").append(ChatColor.GRAY).append(gate.getName())
                        .append(ChatColor.DARK_GRAY).append(" | ").append(ChatColor.GRAY).append(gate.getTimesVisited())
                        .append(ChatColor.DARK_GRAY).append(" | ").append(ChatColor.GRAY)
                        .append("( ").append(gate.getTpCoordinates().getBlockX())
                        .append(",").append(gate.getTpCoordinates().getBlockY())
                        .append(",").append(gate.getTpCoordinates().getBlockZ()).append(" )");
            }
        }
        return out.toString();
    }

    public static String getStargateListFromOwner(UUID ownerUuid) {
        StringBuilder out = new StringBuilder("Stargates Owned by " + gate_ownerName(ownerUuid) + ":");
        for (Stargate gate : stargates) {
            if (ownerUuid.equals(gate.getOwnerUuid())) {
                out.append("\n").append(ChatColor.GRAY).append(gate.getName());
            }
        }
        return out.toString();
    }

    private static String gate_ownerName(UUID ownerUuid) {
        String name = Bukkit.getOfflinePlayer(ownerUuid).getName();
        return (name != null) ? name : ownerUuid.toString();
    }
}