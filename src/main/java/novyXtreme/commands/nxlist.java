package novyXtreme.commands;

import novyXtreme.utils.dbFunctions;
import novyXtreme.utils.messageUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class nxlist implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender.hasPermission("novyxtreme.nxlistall")) {
            // TODO: (parkn) make these send messages not stupid (why are the prefixes done in the method ???)
            // List all Stargates (Non-Verbose)
            if (args.length == 0) {
                messageUtils.sendMessage(dbFunctions.getStargateListToString(), sender);
            } else if (args.length == 1) {
                // List all Stargates (Verbose)
                if (args[0].contains("-v")) {
                    messageUtils.sendMessage(dbFunctions.getStargateListToStringVerbose(null), sender);
                } else {
                    messageUtils.sendMessage(dbFunctions.getStargateListFromOwner(args[0]), sender);
                }
                //TODO check this args length is correct
            } else if (args.length > 1) {
                if (args[0].contains("-v")) {
                    messageUtils.sendMessage(dbFunctions.getStargateListToStringVerbose(args[0]), sender);
                } else if (args[1].contains("-v")) {
                    messageUtils.sendMessage(dbFunctions.getStargateListFromOwnerVerbose(args[0]), sender);
                } else {
                    messageUtils.sendMessage(dbFunctions.getStargateListFromOwner(args[0]), sender);
                }

            }
            return true;
        } else if (sender.hasPermission("novyxtreme.nxlistown")) {
            if (args.length >= 1 && args[0].contains("-v")) {
                messageUtils.sendMessage(dbFunctions.getStargateListFromOwnerVerbose(sender.getName()), sender);
            } else {
                messageUtils.sendMessage(dbFunctions.getStargateListFromOwner(sender.getName()), sender);
            }
            //dbFunctions.getStargateListToString();
            return true;
        } else {
            messageUtils.sendMessage("You do not have permission to use that command!", sender);
        }
        return true;
    }

}
