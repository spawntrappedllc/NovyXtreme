package novyXtreme.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import novyXtreme.NovyXtreme;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class messageUtils {

    private static JavaPlugin plugin;
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    public static void init(JavaPlugin pluginInstance) {
        plugin = pluginInstance;
    }

    public static void sendMessage(String message, CommandSender sender) {
        boolean useMini = plugin.getConfig().getBoolean("chat.use-minimessage", false);
        //todo: less duplication
        if (useMini) {
            Component prefix = MiniMessage.miniMessage().deserialize(
                    plugin.getConfig().getString("chat.prefix", "<dark_purple>[NovyXTreme]</dark_purple><gray> ")
            );
            Component content = MiniMessage.miniMessage().deserialize(message);
            sender.sendMessage(prefix.append(content));
        } else {
            String prefix = ChatColor.translateAlternateColorCodes(
                    '&',
                    plugin.getConfig().getString("chat.prefix", "&5[NovyXTreme]&7 ")
            );
            sender.sendMessage(prefix + message);
        }
    }
}
