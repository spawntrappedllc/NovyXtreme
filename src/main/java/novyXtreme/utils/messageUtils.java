package novyXtreme.utils;

import novyXtreme.NovyXtreme;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class messageUtils {

    private static JavaPlugin plugin;

    public static void init(JavaPlugin pluginInstance)
    {
        plugin = pluginInstance;
    }

    public static void sendMessage(String message, CommandSender sender)
    {
        String prefix = ChatColor.translateAlternateColorCodes(
                '&',
                plugin.getConfig().getString("chat.prefix", "&5[NovyXTreme]&7 ")
        );

        sender.sendMessage(prefix + message);
    }
}
