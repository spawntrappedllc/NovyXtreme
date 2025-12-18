package novyXtreme.commands;

import novyXtreme.NovyXtreme;
import novyXtreme.utils.dbFunctions;
import novyXtreme.utils.messageUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class nxforce implements CommandExecutor {
    Plugin plugin = NovyXtreme.getPlugin(NovyXtreme.class);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("novyxtreme.debug")) {
            messageUtils.sendMessage("You do not have permission to use that command!", sender);
            return true;
        }
        // TODO Pretty sure this isn't required anymore since the active stargate
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            p.removeMetadata("StargateActive", plugin);
        }
        dbFunctions.deactivateAllGates();
        return true;
    }

}
