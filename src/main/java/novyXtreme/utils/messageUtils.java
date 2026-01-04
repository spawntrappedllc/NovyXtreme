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
            try {
                String prefixConfig = plugin.getConfig().getString("chat.prefix", "<dark_purple>[NovyXTreme]</dark_purple><gray> ");

                String convertedMessage = ChatColor.translateAlternateColorCodes('&', message)
                        .replace("§0", "<black>")
                        .replace("§1", "<dark_blue>")
                        .replace("§2", "<dark_green>")
                        .replace("§3", "<dark_aqua>")
                        .replace("§4", "<dark_red>")
                        .replace("§5", "<dark_purple>")
                        .replace("§6", "<gold>")
                        .replace("§7", "<gray>")
                        .replace("§8", "<dark_gray>")
                        .replace("§9", "<blue>")
                        .replace("§a", "<green>")
                        .replace("§b", "<aqua>")
                        .replace("§c", "<red>")
                        .replace("§d", "<light_purple>")
                        .replace("§e", "<yellow>")
                        .replace("§f", "<white>")
                        .replace("§k", "<obf>")
                        .replace("§l", "<bold>")
                        .replace("§m", "<strikethrough>")
                        .replace("§n", "<underlined>")
                        .replace("§o", "<italic>")
                        .replace("§r", "<reset>");

                String fullMessage = prefixConfig + convertedMessage;
                Component component = MINI_MESSAGE.deserialize(fullMessage);
                sender.sendMessage(component);
            } catch (net.kyori.adventure.text.minimessage.ParsingException e) {
                plugin.getLogger().warning("MiniMessage parse failed: " + e.getMessage());
                return;
            }
        } else {
            String prefix = ChatColor.translateAlternateColorCodes(
                    '&',
                    plugin.getConfig().getString("chat.prefix", "&5[NovyXTreme]&7 ")
            );
            sender.sendMessage(prefix + message);
        }
    }
}
