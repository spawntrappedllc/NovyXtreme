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

public class nxlock implements CommandExecutor
{
    Plugin plugin = NovyXtreme.getPlugin(NovyXtreme.class);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (!(sender instanceof Player))
        {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("novyxtreme.nxlock"))
        {
            messageUtils.sendMessage("You do not have permission to use that command!", player);
            return true;
        }

        Stargate targetGate = null;

        if (args.length == 0)
        {
            targetGate = dbFunctions.getActivatedGate(player.getName());
            if (targetGate == null)
            {
                messageUtils.sendMessage("You have not activated a stargate!", player);
                return true;
            }

            if (!player.getUniqueId().equals(targetGate.getOwnerUuid()) && !player.hasPermission("novyxtreme.nxlock.others"))
            {
                messageUtils.sendMessage("You don't own that gate.", player);
                return true;
            }
        }
        else if (args.length == 1)
        {
            targetGate = dbFunctions.getGatebyName(args[0]);
            if (targetGate == null)
            {
                messageUtils.sendMessage("No gate by that name found", player);
                return true;
            }

            if (!player.getUniqueId().equals(targetGate.getOwnerUuid()) && !player.hasPermission("novyxtreme.nxlock.others"))
            {
                messageUtils.sendMessage("You don't own that gate.", player);
                return true;
            }
        }
        else
        {
            messageUtils.sendMessage("Usage: /nxlock [gateName]", player);
            return true;
        }

        boolean newLockedState = !targetGate.isLocked();
        targetGate.setLocked(newLockedState);

        if (newLockedState)
        {
            messageUtils.sendMessage("Gate '" + targetGate.getName() + "' is now locked against incoming dial requests.", player);
        }
        else
        {
            messageUtils.sendMessage("Gate '" + targetGate.getName() + "' is now unlocked.", player);
        }

        return true;
    }
}