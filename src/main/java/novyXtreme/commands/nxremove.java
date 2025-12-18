package novyXtreme.commands;

import novyXtreme.utils.dbFunctions;
import novyXtreme.utils.messageUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class nxremove implements CommandExecutor
{
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if(sender.hasPermission("novyxtreme.nxremoveany"))
        {
            if (args.length != 1)
            {
                messageUtils.sendMessage("Must specify gatename!", sender);
                return true;
            }
            if (dbFunctions.removeGateByName(args[0]))
            {
                messageUtils.sendMessage("Stargate: " + args[0] + " successfully removed.", sender);
                return true;
            } else
            {
                messageUtils.sendMessage("No gate by that name found!", sender);
            }
            return true;
        }else
        if (sender.hasPermission("novyxtreme.nxremoveown"))
        {
            if(!dbFunctions.getGatebyName(args[0]).getOwner().equals(sender.getName()))
            {
                messageUtils.sendMessage("You do not have permission to remove a gate which you do not own!", sender);
                return true;
            }
            if (args.length != 1)
            {
                messageUtils.sendMessage("Must specify gatename!", sender);
                return true;
            }
            if (dbFunctions.removeGateByName(args[0]))
            {
                messageUtils.sendMessage("Stargate: " + args[0] + " successfully removed.", sender);
                return true;
            } else
            {
                messageUtils.sendMessage("No gate by that name found!", sender);
            }
            return true;
        }
        else{messageUtils.sendMessage("You do not have permission to use that command!", sender);}

        return true;
    }
}
