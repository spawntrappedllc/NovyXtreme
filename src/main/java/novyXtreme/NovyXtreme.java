package novyXtreme;

import novyXtreme.Listeners.gateLeverListener;
import novyXtreme.Listeners.portalEnterListener;
import novyXtreme.Listeners.portalTeleportListener;
import novyXtreme.commands.*;
import novyXtreme.utils.dbFunctions;
import novyXtreme.utils.messageUtils;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

public final class NovyXtreme extends JavaPlugin {

    private static NovyXtreme plugin;

    public static NovyXtreme getPlugin() {
        return plugin;
    }

    @Override
    public void onEnable() {
        plugin = this;
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();

        messageUtils.init(this);

        try {
            dbFunctions.initDatabase(this);
        } catch (SQLException e) {
            getLogger().severe("Database error");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try {
            dbFunctions.loadStargates();
        } catch (SQLException e) {
            getLogger().severe("Database error");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(new portalTeleportListener(), this);
        getServer().getPluginManager().registerEvents(new gateLeverListener(),      this);
        getServer().getPluginManager().registerEvents(new portalEnterListener(),    this);

        getCommand("stremove").setExecutor(new nxremove());
        getCommand("stforce") .setExecutor(new nxforce());
        getCommand("stlist")  .setExecutor(new nxlist());
        getCommand("stcomplete").setExecutor(new nxcomplete());
        getCommand("stkeep")  .setExecutor(new nxkeep());
        getCommand("stlock")  .setExecutor(new nxlock());
        getCommand("dial")    .setExecutor(new dial());
        getCommand("stgo")    .setExecutor(new nxgo());
        getCommand("streload").setExecutor(new nxreload());
    }

    @Override
    public void onDisable() {
        try {
            dbFunctions.saveStargates();
        } catch (SQLException e) {
            getLogger().severe("Database error shutdown");
            e.printStackTrace();
        } finally {
            dbFunctions.closeDatabase();
        }
    }
}