package novyXtreme.commands;

import novyXtreme.NovyXtreme;
import novyXtreme.Stargate;
import novyXtreme.utils.dbFunctions;
import novyXtreme.utils.messageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class nxkeep implements CommandExecutor {
    private final Plugin plugin = NovyXtreme.getPlugin(NovyXtreme.class);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }

        Player player = (Player) sender;

        if (args.length != 1) {
            messageUtils.sendMessage("Usage: /nxkeep <gatename>", player);
            return true;
        }

        String keepGateName = args[0];
        Stargate keepGate = dbFunctions.getGatebyName(keepGateName);

        if (keepGate == null) {
            messageUtils.sendMessage("No gate by that name found.", player);
            return true;
        }

        //verify ownership
        if (!dbFunctions.isOwnedBy(player.getUniqueId(), keepGateName)) {
            messageUtils.sendMessage("You do not own that stargate.", player);
            return true;
        }

        dbFunctions.removeAllGatesForOwnerExcept(player.getUniqueId(), keepGateName, false);

        // inform player
        messageUtils.sendMessage("Kept gate: " + keepGateName + ". All other stargates you owned were removed.", player);
        return true;
    }
}
