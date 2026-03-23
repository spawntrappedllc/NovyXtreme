package novyXtreme.commands;

import novyXtreme.utils.dbFunctions;
import novyXtreme.utils.messageUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

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
                    messageUtils.sendMessage(dbFunctions.getStargateListFromOwner(nameToUuid(args[0])), sender);
                }
                //TODO check this args length is correct
            } else if (args.length > 1) {
                if (args[0].contains("-v")) {
                    messageUtils.sendMessage(dbFunctions.getStargateListToStringVerbose(nameToUuid(args[1])), sender);
                } else if (args[1].contains("-v")) {
                    messageUtils.sendMessage(dbFunctions.getStargateListFromOwnerVerbose(nameToUuid(args[0])), sender);
                } else {
                    messageUtils.sendMessage(dbFunctions.getStargateListFromOwner(nameToUuid(args[0])), sender);
                }
            }
            return true;
        } else if (sender.hasPermission("novyxtreme.nxlistown")) {
            UUID senderUuid = ((Player) sender).getUniqueId();
            if (args.length >= 1 && args[0].contains("-v")) {
                messageUtils.sendMessage(dbFunctions.getStargateListFromOwnerVerbose(senderUuid), sender);
            } else {
                messageUtils.sendMessage(dbFunctions.getStargateListFromOwner(senderUuid), sender);
            }
            return true;
        } else {
            messageUtils.sendMessage("You do not have permission to use that command!", sender);
        }
        return true;
    }

    private UUID nameToUuid(String playerName) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(playerName);
        return op.getUniqueId();
    }
}