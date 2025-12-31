package novyXtreme.commands;

import novyXtreme.Stargate;
import novyXtreme.utils.dbFunctions;
import novyXtreme.utils.messageUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class nxgo implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("novyxtreme.nxgo")) {
            messageUtils.sendMessage("You do not have permission to use that command!", player);
            return true;
        }
        if (args.length < 1) {
            messageUtils.sendMessage("Usage: /nxgo [gatename]", player);
            return true;
        }

        Stargate destinationStargate = dbFunctions.getGatebyName(args[0]);

        if (destinationStargate == null) {
            messageUtils.sendMessage("No gate by that name found!", player);
            return true;
        }

        player.teleport(destinationStargate.getTpCoordinates());
        return true;
    }
}
