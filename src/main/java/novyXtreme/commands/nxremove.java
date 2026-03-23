package novyXtreme.commands;

import novyXtreme.utils.dbFunctions;
import novyXtreme.utils.messageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class nxremove implements CommandExecutor
{
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (args.length != 1)
        {
            messageUtils.sendMessage("Must specify gatename!", sender);
            return true;
        }

        if (sender.hasPermission("novyxtreme.nxremoveany"))
        {
            if (dbFunctions.removeGateByName(args[0]))
            {
                messageUtils.sendMessage("Stargate: " + args[0] + " successfully removed.", sender);
            } else
            {
                messageUtils.sendMessage("No gate by that name found!", sender);
            }
            return true;
        }

        if (sender.hasPermission("novyxtreme.nxremoveown"))
        {
            if (!(sender instanceof Player))
            {
                messageUtils.sendMessage("This command can only be run by a player.", sender);
                return true;
            }

            Player player = (Player) sender;

            if (dbFunctions.getGatebyName(args[0]) == null)
            {
                messageUtils.sendMessage("No gate by that name found!", sender);
                return true;
            }

            if (!dbFunctions.getGatebyName(args[0]).getOwnerUuid().equals(player.getUniqueId()))
            {
                messageUtils.sendMessage("You do not have permission to remove a gate which you do not own!", sender);
                return true;
            }

            if (dbFunctions.removeGateByName(args[0]))
            {
                messageUtils.sendMessage("Stargate: " + args[0] + " successfully removed.", sender);
            } else
            {
                messageUtils.sendMessage("No gate by that name found!", sender);
            }
            return true;
        }

        messageUtils.sendMessage("You do not have permission to use that command!", sender);
        return true;
    }
}